// Protocol 2. KV indexes invitation codes only; learning records live in one Durable Object per classroom.
const DAY=86400000,RETENTION=90*DAY;
const json=(data,status=200)=>new Response(JSON.stringify(data),{status,headers:{"content-type":"application/json; charset=utf-8","cache-control":"no-store","x-content-type-options":"nosniff",...(status===429?{"retry-after":"60"}:{})}});
const randomHex=(bytes=24)=>Array.from(crypto.getRandomValues(new Uint8Array(bytes)),x=>x.toString(16).padStart(2,"0")).join("");
const digest=async value=>Array.from(new Uint8Array(await crypto.subtle.digest("SHA-256",new TextEncoder().encode(value))),x=>x.toString(16).padStart(2,"0")).join("");
const bearer=request=>(request.headers.get("authorization")||"").replace(/^Bearer /i,"").trim();
const validId=id=>typeof id==="string"&&/^[a-f0-9]{24}$/.test(id);
class HttpError extends Error {constructor(status,message){super(message);this.status=status}}
const read=async request=>{
 if(Number(request.headers.get("content-length")||0)>12000)throw new HttpError(413,"Payload too large");
 const reader=request.body?.getReader();let size=0;const chunks=[];
 if(reader){try{while(true){const {done,value}=await reader.read();if(done)break;size+=value.byteLength;if(size>12000){await reader.cancel();throw new HttpError(413,"Payload too large")}chunks.push(value)}}finally{reader.releaseLock()}}
 const data=new Uint8Array(size);let offset=0;for(const chunk of chunks){data.set(chunk,offset);offset+=chunk.length}
 try{return JSON.parse(new TextDecoder().decode(data)||"{}")}catch{throw new HttpError(400,"Invalid JSON")}
};
const integer=(v,max)=>Number.isInteger(v)&&v>=0&&v<=max;
function publicReport(r){
 if(!r||r.format!=="LinguaFreeStudyReport"||r.version!==1||typeof r.student!=="string"||!r.student.trim()||r.student.length>80||!integer(r.completedLessons,300)||!integer(r.xp,10000000)||!integer(r.studyDaysLast7,7)||!integer(r.studyDaysLast30,30))throw new HttpError(400,"Invalid report");
 if(!Array.isArray(r.skills)||r.skills.length!==4||r.skills.some(x=>!x||!integer(x.attempts,1000000)||!integer(x.correct,x.attempts)))throw new HttpError(400,"Invalid skills");
 if(typeof r.lastStudyDate!=="string"||!/^$|^\d{4}-\d{2}-\d{2}$/.test(r.lastStudyDate))throw new HttpError(400,"Invalid study date");
 return {format:r.format,version:1,student:r.student.trim(),completedLessons:r.completedLessons,totalLessons:300,xp:r.xp,
  studyDaysLast7:r.studyDaysLast7,studyDaysLast30:r.studyDaysLast30,lastStudyDate:r.lastStudyDate,
  grammarTopicsCompleted:integer(r.grammarTopicsCompleted,1000)?r.grammarTopicsCompleted:0,
  skills:r.skills.map((x,i)=>({name:["Чтение и перевод","Аудирование","Письмо","Говорение"][i],attempts:x.attempts,correct:x.correct}))};
}

export default {
 async fetch(request,env){
  try{
   const url=new URL(request.url),path=url.pathname;
   if(url.protocol!=="https:")return json({error:"HTTPS required"},400);
   if(!env.CLASSROOM_KV||!env.CLASSROOMS||!env.CLASSROOM_RATE_LIMITER)return json({error:"Server bindings incomplete"},503);
   if(request.method==="GET"&&path==="/health")return json({ok:true,protocol:2});
   const routes={"/teacher/create":"POST","/teacher/reports":"GET","/teacher/code":"POST","/teacher/remove":"POST","/student/join":"POST","/student/sync":"POST","/student/revoke":"POST"};
   if(!routes[path])return json({error:"Not found"},404);
   if(request.method!==routes[path])return json({error:"Method not allowed"},405);
   const token=bearer(request);
   const publicRoute=path==="/teacher/create"||path==="/student/join";
   const actor=!publicRoute&&token?await digest(token):"anonymous:"+(request.headers.get("cf-connecting-ip")||"unknown");
   if(!(await env.CLASSROOM_RATE_LIMITER.limit({key:actor})).success)return json({error:"Too many requests"},429);
   const body=request.method==="POST"?await read(request):{};
   let classroomId;
   if(path==="/teacher/create"){
    // An operator must explicitly open creation after configuring consent and privacy procedures.
    if(env.ALLOW_CLASSROOM_CREATION!=="true")return json({error:"Classroom creation disabled"},403);
    classroomId=randomHex(12);body.classroomId=classroomId;
   }else if(path==="/student/join"){
    const code=String(body.code||"").trim().toUpperCase();
    if(!/^[A-F0-9]{16}$/.test(code))return json({error:"Invalid code"},400);
    classroomId=await env.CLASSROOM_KV.get("code:"+code);body.code=code;
    if(!classroomId)return json({error:"Code expired or invalid"},404);
   }else if(path.startsWith("/teacher/"))classroomId=url.searchParams.get("classroomId")||body.classroomId;
   else classroomId=token.split(".")[0];
   if(!validId(classroomId))return json({error:"Invalid classroom or obsolete credentials"},401);
   const stub=env.CLASSROOMS.get(env.CLASSROOMS.idFromName(classroomId));
   const headers=new Headers(request.headers);headers.delete("content-length");
   return await stub.fetch(new Request(url,{method:request.method,headers,body:request.method==="POST"?JSON.stringify(body):undefined}));
  }catch(error){return json({error:error instanceof HttpError?error.message:"Request failed"},error.status||503)}
 }
};

export class Classroom {
 constructor(state,env){this.state=state;this.storage=state.storage;this.env=env}
 async fetch(request){
  // No request can interleave a read/hash/write sequence with a revocation or another snapshot.
  return this.state.blockConcurrencyWhile(async()=>{
   try{return await this.handle(request)}catch(error){return json({error:error instanceof HttpError?error.message:"Request failed"},error.status||503)}
  });
 }
 async handle(request){
  const url=new URL(request.url),path=url.pathname,body=request.method==="POST"?await read(request):{};
  const s=this.storage,token=bearer(request),meta=await s.get("meta");
  if(path==="/teacher/create"){
   if(meta)throw new HttpError(409,"Classroom already exists");
   if(!validId(body.classroomId))throw new HttpError(400,"Invalid classroom id");
   const teacherToken=randomHex(32),code=randomHex(8).toUpperCase();
   await this.env.CLASSROOM_KV.put("code:"+code,body.classroomId,{expirationTtl:86400});
   await s.put("meta",{classroomId:body.classroomId,hash:await digest(teacherToken),code,codeExpires:Date.now()+DAY,created:Date.now()});
   await s.setAlarm(Date.now()+DAY);
   return json({classroomId:body.classroomId,teacherToken,code,expiresAt:new Date(Date.now()+DAY).toISOString(),protocol:2});
  }
  if(!meta)throw new HttpError(404,"Classroom unavailable");
  if(path.startsWith("/teacher/")){
   if(!token||await digest(token)!==meta.hash)throw new HttpError(401,"Unauthorized");
   if(path==="/teacher/reports"){
    const cursor=url.searchParams.get("cursor");
    if(cursor&&!/^report:[a-f0-9]{24}$/.test(cursor))throw new HttpError(400,"Invalid cursor");
    const records=await s.list({prefix:"report:",limit:101,...(cursor?{startAfter:cursor}:{})});
    const rows=Array.from(records.entries()),more=rows.length>100;const page=rows.slice(0,100);
    return json({reports:page.map(([,value])=>value),more,cursor:more?page.at(-1)[0]:null});
   }
   if(path==="/teacher/code"){
    const code=randomHex(8).toUpperCase(),expires=Date.now()+DAY;
    await this.env.CLASSROOM_KV.put("code:"+code,meta.classroomId,{expirationTtl:86400});
    await s.put("meta",{...meta,code,codeExpires:expires});
    await this.env.CLASSROOM_KV.delete("code:"+meta.code);
    return json({code,expiresAt:new Date(expires).toISOString()});
   }
   if(path==="/teacher/remove"){
    if(!validId(body.studentId))throw new HttpError(400,"Invalid student id");
    const record=await s.get("student:"+body.studentId);
    if(!record)throw new HttpError(404,"Student unavailable");
    await this.revoke(body.studentId,record);
    return json({ok:true});
   }
  }
  if(path==="/student/join"){
   if(body.consentVersion!==1||body.consentConfirmed!==true)throw new HttpError(400,"Consent required");
   const name=typeof body.name==="string"?body.name.trim():"";
   if(!name||name.length>80)throw new HttpError(400,"Invalid name");
   if(body.code!==meta.code||Date.now()>=meta.codeExpires)throw new HttpError(404,"Code expired or invalid");
   // Cap the prototype's roster; no unbounded scans or uncontrolled per-class enrolment.
   if((await s.list({prefix:"student:",limit:201})).size>=200)throw new HttpError(409,"Classroom is full");
   const studentId=randomHex(12),studentToken=meta.classroomId+"."+randomHex(32),now=Date.now();
   await s.put({["student:"+studentId]:{hash:await digest(studentToken),lastActive:now,revision:0,consentVersion:1},
    ["report:"+studentId]:{studentId,joinedAt:new Date(now).toISOString(),pending:true,report:{student:name}}});
   await s.setAlarm(Date.now()+DAY);
   return json({studentId,studentToken,protocol:2});
  }
  if(path==="/student/sync"||path==="/student/revoke"){
   if(!validId(body.studentId))throw new HttpError(400,"Invalid student id");
   const record=await s.get("student:"+body.studentId);
   if(!record||!token||await digest(token)!==record.hash)throw new HttpError(401,"Unauthorized");
   if(path==="/student/revoke"){await this.revoke(body.studentId,record);return json({ok:true})}
   if(record.revoked)throw new HttpError(401,"Access revoked");
   if(!Number.isSafeInteger(body.revision)||body.revision<1||typeof body.snapshotId!=="string"||!/^[a-f0-9]{32}$/.test(body.snapshotId))throw new HttpError(400,"Invalid revision or snapshot");
   if(body.revision<record.revision)return json({ok:true,stale:true,acceptedRevision:record.revision});
   if(body.revision===record.revision){
    if(body.snapshotId!==record.snapshotId)throw new HttpError(409,"Snapshot revision conflict");
    return json({ok:true,duplicate:true,acceptedRevision:record.revision});
   }
   const safe=publicReport(body.report),key="report:"+body.studentId,old=await s.get(key),now=Date.now();
   await s.put({["student:"+body.studentId]:{...record,lastActive:now,revision:body.revision,snapshotId:body.snapshotId},
    [key]:{studentId:body.studentId,joinedAt:old?.joinedAt,openedAt:new Date(now).toISOString(),report:safe,
     previousXp:old?.report?.xp??null,previousLessons:old?.report?.completedLessons??null}});
   return json({ok:true,acceptedRevision:body.revision});
  }
  throw new HttpError(404,"Not found");
 }
 async revoke(id,record){
  await this.storage.transaction(async tx=>{
   await tx.put("student:"+id,{hash:record.hash,revoked:true,lastActive:Date.now()});
   await tx.delete("report:"+id);
  });
 }
 async alarm(){
  return this.state.blockConcurrencyWhile(async()=>{
  // A classroom has at most 200 entries. Expire inactive learning records and revocation tombstones.
  const students=await this.storage.list({prefix:"student:"});
  for(const [key,record] of students){if(record.lastActive<Date.now()-RETENTION){await this.storage.delete([key,"report:"+key.slice(8)])}}
  if(students.size)await this.storage.setAlarm(Date.now()+DAY);
  });
 }
}
