package vn.edu.onlineexam.service;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.sql.SQLException;
import java.util.concurrent.*;
import vn.edu.onlineexam.util.ConnectionFactory;

@WebListener
public final class AttemptExpiry implements ServletContextListener {
    private ScheduledExecutorService timer;
    private long cursor;
    private ScheduledExecutorService contacts;
    @Override public void contextInitialized(ServletContextEvent event){
        var service=new AttemptService(new JdbcUnitOfWork(()->ConnectionFactory.fromEnvironment().open()));
        var contact=new AttemptContactService(new JdbcUnitOfWork(()->ConnectionFactory.fromEnvironment().open()));
        contacts=Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon(true).name("online-exam-contact").factory());
        contacts.scheduleWithFixedDelay(()->{
            try{contact.inspect();}
            catch(SQLException | RuntimeException failure){event.getServletContext().log("Attempt contact inspection temporarily unavailable.");}
        },30,30,TimeUnit.SECONDS);
        timer=Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon(true).name("online-exam-deadlines").factory());
        timer.scheduleWithFixedDelay(()->{
            try{cursor=service.expireBatch(cursor);}
            catch(SQLException | RuntimeException failure){event.getServletContext().log("Không thể hoàn tất kiểm tra lượt hết hạn. Máy chủ sẽ thử lại.");}
        },15,15,TimeUnit.SECONDS);
    }
    @Override public void contextDestroyed(ServletContextEvent event){
        if(contacts!=null)contacts.shutdownNow();
        if(timer!=null)timer.shutdownNow();
    }
}
