// Cloudflare Worker + KV. Deploy with: npx wrangler deploy
// Bind KV namespace as CLASSROOM_KV in wrangler.toml.
// Only minimal voluntary learning statistics are stored; no GPS or device identifiers.
const json=(data,status=200)=>new Response(JSON.stringify(data),{status,headers:{"content-type":"application/json; charset=utf-8","cache-control":"no-store"}});
const randomHex=(bytes=24)=>Array.from(crypto.getRandomValues(new Uint8Array(bytes)),x=>x.toString(16).padStart(2,"0")).join("");
const digest=async value=>Array.from(new Uint8Array(await crypto.subtle.digest("SHA-256",new TextEncoder().encode(value))),x=>x.toString(16).padStart(2,"0")).join("");
const bearer=request=>(request.headers.get("authorization")||"").replace(/^Bearer /i,"").trim();
const read=async request=>{if(Number(request.headers.get("content-length")||0)>12000)throw Error("Payload too large");const s=await request.text();if(s.length>12000)throw Error("Payload too large");return JSON.parse(s)};
const validateReport=r=>r&&r.format==="LinguaFreeStudyReport"&&r.version===1&&typeof r.student==="string"&&r.student.trim().length>0&&r.student.length<=80&&Number.isInteger(r.completedLessons)&&r.completedLessons>=0&&r.completedLessons<=300&&Number.isInteger(r.xp)&&r.xp>=0&&r.xp<=10000000&&Number.isInteger(r.studyDaysLast7)&&r.studyDaysLast7>=0&&r.studyDaysLast7<=7&&Number.isInteger(r.studyDaysLast30)&&r.studyDaysLast30>=0&&r.studyDaysLast30<=30;
const validId=id=>/^[a-f0-9]{24}$/.test(id);
const publicReport=r=>({
 format:"LinguaFreeStudyReport",version:1,
 student:r.student.trim().slice(0,80),completedLessons:r.completedLessons,totalLessons:300,xp:r.xp,
 studyDaysLast7:r.studyDaysLast7,studyDaysLast30:r.studyDaysLast30,
 lastStudyDate:typeof r.lastStudyDate==="string"?r.lastStudyDate.slice(0,10):"",
 skills:Array.isArray(r.skills)?r.skills.slice(0,4).map(x=>({name:String(x.name||"").slice(0,40),attempts:Math.max(0,Math.min(1000000,Number(x.attempts)||0)),correct:Math.max(0,Math.min(1000000,Number(x.correct)||0))})):[]
});
export default {
 async fetch(request,env){
  const url=new URL(request.url),path=url.pathname,kv=env.CLASSROOM_KV;
  if(!kv)return json({error:"KV not configured"},503);
  if(request.method==="GET"&&path==="/health")return json({ok:true});
  try {
   if(request.method==="POST"&&path==="/teacher/create"){
    if(env.CLASSROOM_CREATION_DISABLED==="true")return json({error:"Classroom creation disabled"},403);
    const classroomId=randomHex(12),teacherToken=randomHex(32),code=randomHex(8).toUpperCase();
    await kv.put("class:"+classroomId,JSON.stringify({hash:await digest(teacherToken),created:Date.now()}));
    await kv.put("code:"+code,classroomId,{expirationTtl:86400});
    return json({classroomId,teacherToken,code,expiresInHours:24});
   }
   if(request.method==="POST"&&path==="/student/join"){
    const body=await read(request),code=String(body.code||"").trim().toUpperCase(),name=String(body.name||"").trim();
    if(!/^[A-F0-9]{16}$/.test(code)||!name||name.length>80)return json({error:"Invalid pairing code or name"},400);
    const classroomId=await kv.get("code:"+code);
    if(!classroomId)return json({error:"Code expired or invalid"},404);
    if(!await kv.get("class:"+classroomId))return json({error:"Classroom unavailable"},404);
    const studentId=randomHex(12),studentToken=randomHex(32);
    await kv.put("student:"+studentId,JSON.stringify({classroomId,hash:await digest(studentToken)}));
    return json({studentId,studentToken});
   }
   if(request.method==="POST"&&path==="/student/sync"){
    const body=await read(request),studentId=String(body.studentId||""),token=bearer(request);
    if(!validId(studentId))return json({error:"Invalid student id"},400);
    const record=await kv.get("student:"+studentId,"json");
    if(!record||!token||await digest(token)!==record.hash)return json({error:"Unauthorized"},401);
    if(!validateReport(body.report))return json({error:"Invalid report"},400);
    const safe=publicReport(body.report);
    const key="report:"+record.classroomId+":"+studentId;
    const old=await kv.get(key,"json");
    // "openedAt" is assigned by the server; never trust client clocks for notifications.
    await kv.put(key,JSON.stringify({studentId,openedAt:new Date().toISOString(),report:safe,previousXp:old?.report?.xp??null,previousLessons:old?.report?.completedLessons??null}));
    return json({ok:true});
   }
   if(request.method==="GET"&&path==="/teacher/reports"){
    const classroomId=url.searchParams.get("classroomId")||"",token=bearer(request);
    if(!validId(classroomId))return json({error:"Invalid classroom id"},400);
    const record=await kv.get("class:"+classroomId,"json");
    if(!record||!token||await digest(token)!==record.hash)return json({error:"Unauthorized"},401);
    const entries=await kv.list({prefix:"report:"+classroomId+":",limit:1000});
    const reports=(await Promise.all(entries.keys.map(x=>kv.get(x.name,"json")))).filter(Boolean);
    return json({reports,more:!entries.list_complete});
   }
   if(request.method==="POST"&&path==="/student/revoke"){
    const body=await read(request),studentId=String(body.studentId||""),token=bearer(request);
    const record=await kv.get("student:"+studentId,"json");
    if(!record||!token||await digest(token)!==record.hash)return json({error:"Unauthorized"},401);
    await kv.delete("report:"+record.classroomId+":"+studentId);
    await kv.delete("student:"+studentId);
    return json({ok:true});
   }
   return json({error:"Not found"},404);
  }catch(e){return json({error:"Request failed"},400)}
 }
};
