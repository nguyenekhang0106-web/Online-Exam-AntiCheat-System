package vn.edu.onlineexam.service;

import java.sql.SQLException;
import java.util.Set;
import vn.edu.onlineexam.dao.UserDao;
import vn.edu.onlineexam.model.*;
import vn.edu.onlineexam.model.UserDirectory.*;

public final class UserManagementService {
    private final JdbcUnitOfWork jdbc;
    private final UserDao dao = new UserDao();
    public UserManagementService(JdbcUnitOfWork jdbc) { this.jdbc = jdbc; }

    public static final class Conflict extends RuntimeException {
        public Conflict(String message) { super(message); }
    }
    public static void requireAdmin(AuthUser actor) {
        if (actor == null || !actor.roles().contains("ADMIN")) throw new SecurityException();
    }
    private static String field(String s,int limit,boolean required) {
        String v=s==null ? "" : s.strip();
        if ((required && v.isEmpty()) || v.length()>limit || v.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Kiểm tra trường bắt buộc, độ dài và ký tự điều khiển.");
        return v.isEmpty() ? null : v;
    }
    private static Profile validate(Profile raw) {
        String name=field(raw.name(),150,true), email=field(raw.email(),254,true);
        String code=field(raw.code(),30,false), phone=field(raw.phone(),20,false);
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalArgumentException("Email không hợp lệ.");
        if (code!=null && !code.matches("[A-Za-z0-9_-]+")) throw new IllegalArgumentException("Mã chỉ gồm chữ Latin, số, gạch dưới hoặc gạch ngang.");
        if (phone!=null && !phone.matches("[+0-9() .-]+")) throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
        return new Profile(name,email,code,phone);
    }
    private static void checkId(long id) {
        if (id<1) throw new IllegalArgumentException("ID không hợp lệ.");
    }
    private static Conflict duplicate() { return new Conflict("Email hoặc mã người dùng đã tồn tại."); }
    public void changeStatus(AuthUser actor,long id,boolean lock,String expectedVersion) throws SQLException {
        requireAdmin(actor);
        checkId(id);
        java.time.LocalDateTime version;
        try { version=java.time.LocalDateTime.parse(expectedVersion==null?"":expectedVersion); }
        catch(java.time.format.DateTimeParseException bad) { throw new IllegalArgumentException("Phiên bản hồ sơ không hợp lệ."); }
        jdbc.transaction(c -> {
            var admins=dao.lockActiveAdmins(c);
            if(!admins.contains(actor.id())) throw new SecurityException();
            String status=dao.lockStatus(c,id);
            if(status==null) throw new java.util.NoSuchElementException();
            if(lock && admins.contains(id) && admins.size()==1) throw new Conflict("Không thể khóa quản trị viên hoạt động cuối cùng.");
            if(lock && actor.id()==id) throw new Conflict("Bạn không thể tự khóa tài khoản.");
            String expected=lock?"ACTIVE":"LOCKED";
            if(!expected.equals(status) || !dao.changeStatus(c,id,expected,lock?"LOCKED":"ACTIVE",version))
                throw new Conflict("Trạng thái hoặc hồ sơ đã thay đổi. Hãy tải lại danh sách.");
            if(lock) dao.revokeSessions(c,id);
            dao.audit(c,actor.id(),id,lock?"USER_LOCK":"USER_UNLOCK");
            return null;
        });
    }
    private <T> T write(JdbcUnitOfWork.Work<T> work) throws SQLException {
        try { return jdbc.transaction(work); }
        catch (SQLException e) { if(e.getErrorCode()==1062) throw duplicate(); throw e; }
    }
    public Detail detail(AuthUser actor,long id) throws SQLException {
        requireAdmin(actor); checkId(id);
        Detail d=jdbc.read(c->dao.find(c,id,false));
        if(d==null) throw new java.util.NoSuchElementException();
        return d;
    }
    public long create(AuthUser actor,Profile raw,char[] password,char[] confirm,String role) throws SQLException {
        requireAdmin(actor);
        Profile p=validate(raw);
        if(role==null || !Set.of("ADMIN","TEACHER","STUDENT").contains(role))
            throw new IllegalArgumentException("Chọn vai trò ban đầu.");
        if(password==null || password.length<12 || password.length>1024 || new String(password).isBlank()
                || !java.util.Arrays.equals(password,confirm)) throw new IllegalArgumentException("Mật khẩu cần 12–1024 ký tự và xác nhận phải khớp.");
        String hash=vn.edu.onlineexam.util.PasswordHasher.hash(password);
        return write(c->{
            if(dao.duplicate(c,p,0)) throw duplicate();
            long id=dao.insert(c,p,hash);
            if(!dao.assignInitialRole(c,id,role,actor.id())) throw new Conflict("Vai trò chưa có trong danh mục.");
            dao.audit(c,actor.id(),id,"USER_CREATE");
            dao.audit(c,actor.id(),id,"ROLE_GRANT_"+role);
            return id;
        });
    }
    public void update(AuthUser actor,long id,Profile raw,String version) throws SQLException {
        requireAdmin(actor); checkId(id);
        Profile p=validate(raw);
        write(c->{
            Detail current=dao.find(c,id,true);
            if(current==null) throw new java.util.NoSuchElementException();
            if(!current.version().toString().equals(version)) throw new Conflict("Hồ sơ đã thay đổi. Mở lại trang sửa trước khi lưu.");
            if(current.verified() && !current.profile().email().equals(p.email()))
                throw new Conflict("Đổi email đã xác minh cần quy trình xác minh riêng.");
            if(dao.duplicate(c,p,id)) throw duplicate();
            dao.update(c,id,p); dao.audit(c,actor.id(),id,"USER_PROFILE_UPDATE");
            return null;
        });
    }

    public UserDirectory search(AuthUser actor,String keyword,String role,String status,String page,String size,
                                String sort,String direction) throws SQLException {
        String term = keyword == null ? "" : keyword.strip();
        requireAdmin(actor);
        String roleCode = role == null ? "" : role, state = status == null ? "" : status;
        if (term.length()>254 || !Set.of("","ADMIN","TEACHER","STUDENT").contains(roleCode)
                || !Set.of("","ACTIVE","INACTIVE","LOCKED").contains(state)) throw new IllegalArgumentException();
        int requested = page == null ? 1 : Integer.parseInt(page);
        int limit = size == null ? 20 : Integer.parseInt(size);
        if (requested < 1 || limit < 1 || limit > 100) throw new IllegalArgumentException();
        Query query = new Query(term,roleCode,state,limit,Sort.valueOf(sort == null ? "created" : sort),
                Direction.valueOf(direction == null ? "DESC" : direction));
        return jdbc.read(c -> {
            long total = dao.count(c,query);
            long pages = total == 0 ? 0 : 1 + (total-1)/limit;
            long current = Math.min(requested,Math.max(1,pages));
            return new UserDirectory(dao.list(c,query,(current-1)*limit),total,pages,current,query);
        });
    }
}
