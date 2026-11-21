import {spawn} from "node:child_process";
import {once} from "node:events";
import {mkdtemp,readFile} from "node:fs/promises";
import {join} from "node:path";
import {setTimeout as pause} from "node:timers/promises";

let input="";
for await(const part of process.stdin) input+=part;
const {base,id,question,correct,wrong,password}=JSON.parse(input);
const origin=new URL(base);
if(origin.protocol!=="http:" || origin.hostname!=="127.0.0.1" || !/^jdbc:mysql:\/\/127\.0\.0\.1:13367\/role_test_[\da-f]{32}$/.test(process.env.DB_URL||"")) throw Error("Scratch fixture required");
const profile=await mkdtemp(join(process.cwd(),"target","attempt-edge-"));
const edge=spawn(join(process.env["ProgramFiles(x86)"],"Microsoft","Edge","Application","msedge.exe"),[
    "--headless","--disable-gpu","--disable-extensions","--no-first-run","--remote-debugging-port=0",
    "--remote-debugging-address=127.0.0.1","--user-data-dir="+profile,"about:blank"
],{stdio:"ignore",windowsHide:true});
let ws;
async function until(read,label){
    for(let i=0;i<120;i++){try{const v=await read();if(v)return v;}catch{} await pause(120);}
    throw Error("Browser check failed: "+label);
}
try{
    const port=await until(async()=>(await readFile(join(profile,"DevToolsActivePort"),"utf8")).split(/\r?\n/)[0],"Edge startup");
    const targets=await(await fetch("http://127.0.0.1:"+port+"/json/list")).json();
    ws=new WebSocket(targets.find(t=>t.type==="page").webSocketDebuggerUrl);
    await once(ws,"open");
    const calls=new Map();let sequence=0;
    ws.addEventListener("message",event=>{const msg=JSON.parse(event.data);if(calls.has(msg.id)){calls.get(msg.id)(msg);calls.delete(msg.id);}});
    async function call(method,params={}){
        const id=++sequence;const result=new Promise(resolve=>calls.set(id,resolve));
        ws.send(JSON.stringify({id,method,params}));const message=await result;
        if(message.error)throw Error("Browser command failed: "+method);
        return message.result;
    }
    async function evaluate(expression){const r=await call("Runtime.evaluate",{expression,returnByValue:true});if(r.exceptionDetails)throw Error("Browser evaluation failed");return r.result.value;}
    const navigate=path=>call("Page.navigate",{url:new URL(path,origin).href});
    await call("Network.enable");await navigate("login");
    await until(()=>evaluate("!!document.querySelector('input[type=password]')"),"login form");
    await evaluate("document.querySelector('input[name=email]').value='student@subject.invalid';document.querySelector('input[type=password]').value="+JSON.stringify(password)+";document.querySelector('form').requestSubmit()");
    await until(()=>evaluate("location.pathname.endsWith('/account')"),"student login");
    await navigate("student/attempts?action=take&id="+id);
    const form="document.getElementById('q"+Number(question)+"')";
    const choice=n=>form+".querySelector('input[value=\""+Number(n)+"\"][name=option]')";
    await until(()=>evaluate("!!"+form),"paper");
    for(const width of [390,1280]){
        await call("Emulation.setDeviceMetricsOverride",{width,height:800,deviceScaleFactor:1,mobile:false});
        if(!await evaluate("document.documentElement.scrollWidth<=innerWidth"))throw Error("Paper overflow");
    }
    await call("Network.emulateNetworkConditions",{offline:true,latency:0,downloadThroughput:-1,uploadThroughput:-1});
    await evaluate(choice(wrong)+".click()");
    await until(()=>evaluate(form+".querySelector('output').textContent.includes('kết nối')"),"offline feedback");
    await call("Network.emulateNetworkConditions",{offline:false,latency:0,downloadThroughput:-1,uploadThroughput:-1});
    await evaluate(form+".requestSubmit()");
    await until(()=>evaluate(form+".querySelector('output').textContent==='Đã lưu'"),"retry save");
    await call("Page.reload");await until(()=>evaluate(choice(wrong)+".checked"),"restore after reload");
    await evaluate(choice(correct)+".click()");
    await until(()=>evaluate(form+".querySelector('output').textContent==='Đã lưu'"),"autosave");
    await evaluate("document.querySelector('#confirm').click()");
    await until(()=>evaluate("document.querySelector('h1')?.textContent==='Xác nhận nộp bài'"),"confirmation");
    console.log("PASS Edge live: mobile/desktop paper, offline save, retry, reload, autosave, confirmation");
}finally{ws?.close();edge.kill();}
