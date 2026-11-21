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
    await call("Page.enable");await call("Network.enable");await navigate("login");
    await until(()=>evaluate("!!document.querySelector('input[type=password]')"),"login form");
    await evaluate("document.querySelector('input[name=email]').value='student@subject.invalid';document.querySelector('input[type=password]').value="+JSON.stringify(password)+";document.querySelector('form').requestSubmit()");
    await until(()=>evaluate("location.pathname.endsWith('/account')"),"student login");
    await navigate("student/attempts?action=take&id="+id);
    const form="document.getElementById('q"+Number(question)+"')";
    const choice=n=>form+".querySelector('input[value=\""+Number(n)+"\"][name=option]')";
    await call("Page.bringToFront");
    await until(()=>evaluate("document.readyState==='complete' && !!"+form),"paper");
    const monitored=await evaluate("document.querySelector('#monitor').dataset.focus==='true'");
    let pasteResponses=0;
    if(monitored){
        const accepted=new Set(), codes=new Map();
        const fields=new Set(["attempt","csrf","key","code","occurred"]);
        let unexpectedField=false;
        ws.addEventListener("message",event=>{
            const msg=JSON.parse(event.data),p=msg.params;
            if(msg.method==="Network.requestWillBeSent" && p.request.url.endsWith("/proctoring/events")){
                const body=new URLSearchParams(p.request.postData);
                if([...body.keys()].some(field=>!fields.has(field)))unexpectedField=true;
                codes.set(p.requestId,body.get("code"));
            }
            if(msg.method==="Network.responseReceived" && [201,204].includes(p.response.status)){
                accepted.add(codes.get(p.requestId));
                if(codes.get(p.requestId)==="PASTE_ATTEMPT")pasteResponses++;
            }
        });
        await call("Runtime.evaluate",{expression:"document.querySelector('#enter-fullscreen').click()",userGesture:true});
        await until(()=>evaluate("!!document.fullscreenElement"),"enter fullscreen");
        await evaluate("document.exitFullscreen();true");
        await until(()=>accepted.has("FULLSCREEN_EXIT"),"fullscreen exit delivered");
        await call("Runtime.evaluate",{expression:"getSelection().selectAllChildren("+form+".querySelector('fieldset p'));document.execCommand('copy')",userGesture:true});
        await until(()=>accepted.has("COPY_ATTEMPT"),"copy delivered");
        await evaluate(form+".dispatchEvent(new Event('paste',{bubbles:true}))");
        await until(()=>accepted.has("PASTE_ATTEMPT"),"paste delivered");
        const other=await call("Target.createTarget",{url:"about:blank"});
        await call("Target.activateTarget",{targetId:other.targetId});
        await pause(1200);
        const nativeHidden=await evaluate("document.hidden");
        await call("Page.bringToFront");
        if(!nativeHidden && !accepted.has("TAB_HIDDEN") && ![...codes.values()].includes("TAB_HIDDEN")){
            console.log("Headless tab visibility fallback: simulated visibility transition");
            await evaluate("Object.defineProperty(document,'hidden',{configurable:true,value:true});document.dispatchEvent(new Event('visibilitychange'));delete document.hidden;document.dispatchEvent(new Event('visibilitychange'))");
        }
        await until(()=>accepted.has("TAB_HIDDEN"),"tab hidden delivered");
        await call("Target.closeTarget",{targetId:other.targetId});
        const beforeBlur=[...codes.values()].filter(code=>code==="WINDOW_BLUR").length;
        await evaluate("window.dispatchEvent(new Event('focus'));for(let i=0;i<20;i++)window.dispatchEvent(new Event('blur'))");
        await until(()=>accepted.has("WINDOW_BLUR"),"separate visible blur delivered");
        if([...codes.values()].filter(code=>code==="WINDOW_BLUR").length>beforeBlur+1)throw Error("Repeated blur flooded intake");
        if(unexpectedField)throw Error("Unexpected proctoring payload field");
        console.log("PASS browser detector delivery: fullscreen, copy, paste, visibility and focus");
    }
    for(const width of [390,1280]){
        await call("Emulation.setDeviceMetricsOverride",{width,height:800,deviceScaleFactor:1,mobile:false});
        if(!await evaluate("document.documentElement.scrollWidth<=innerWidth"))throw Error("Paper overflow");
    }
    await call("Network.emulateNetworkConditions",{offline:true,latency:0,downloadThroughput:-1,uploadThroughput:-1});
    if(monitored)await evaluate(form+".dispatchEvent(new Event('paste',{bubbles:true}))");
    await evaluate(choice(wrong)+".click()");
    await until(()=>evaluate(form+".querySelector('output').textContent.includes('kết nối')"),"offline feedback");
    await call("Network.emulateNetworkConditions",{offline:false,latency:0,downloadThroughput:-1,uploadThroughput:-1});
    await evaluate(form+".requestSubmit()");
    await until(()=>evaluate(form+".querySelector('output').textContent==='Đã lưu'"),"retry save");
    if(monitored){
        await until(()=>pasteResponses===2,"offline event delivered on reconnect");
        await call("Page.addScriptToEvaluateOnNewDocument",{source:"Object.defineProperty(document,'fullscreenEnabled',{value:false,configurable:true})"});
    }
    await call("Page.reload");await until(()=>evaluate(choice(wrong)+".checked"),"restore after reload");
    if(monitored){
        await until(()=>evaluate("document.querySelector('#enter-fullscreen')?.hidden"),"unsupported fullscreen");
        let paused=false;
        ws.addEventListener("message",e=>{if(JSON.parse(e.data).method==="Fetch.requestPaused")paused=true;});
        await call("Fetch.enable",{patterns:[{urlPattern:"*/student/proctoring/events",requestStage:"Request"}]});
        await evaluate(form+".dispatchEvent(new Event('paste',{bubbles:true}))");
        await until(()=>paused,"slow proctoring request pending");
    }
    await evaluate(choice(correct)+".click()");
    await until(()=>evaluate(form+".querySelector('output').textContent==='Đã lưu'"),"autosave");
    await evaluate("document.querySelector('#confirm').click()");
    await until(()=>evaluate("document.querySelector('h1')?.textContent==='Xác nhận nộp bài'"),"confirmation");
    console.log("PASS Edge live: mobile/desktop paper, offline save, retry, reload, autosave, confirmation");
}finally{ws?.close();edge.kill();}
