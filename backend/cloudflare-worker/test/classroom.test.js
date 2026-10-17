import test from 'node:test';
import assert from 'node:assert/strict';
import worker,{Classroom} from '../src/index.js';

class Storage {
 constructor(){this.data=new Map();this.alarm=null}
 async get(key){return structuredClone(this.data.get(key))}
 async put(key,value){if(typeof key==='object'){for(const [k,v] of Object.entries(key))this.data.set(k,structuredClone(v))}else this.data.set(key,structuredClone(value))}
 async delete(keys){for(const key of Array.isArray(keys)?keys:[keys])this.data.delete(key)}
 async list({prefix='',limit=Infinity,startAfter=''}={}){return new Map([...this.data].sort(([a],[b])=>a.localeCompare(b)).filter(([k])=>k.startsWith(prefix)&&k>startAfter).slice(0,limit).map(([k,v])=>[k,structuredClone(v)]))}
 async setAlarm(value){this.alarm=value}
 async transaction(fn){const before=structuredClone(this.data);try{return await fn(this)}catch(e){this.data=before;throw e}}
}
function environment(){
 const kv=new Map(),objects=new Map();
 const env={ALLOW_CLASSROOM_CREATION:'true',CLASSROOM_KV:{get:async k=>kv.get(k),put:async(k,v)=>{kv.set(k,v)},delete:async k=>{kv.delete(k)}},CLASSROOM_RATE_LIMITER:{limit:async()=>({success:true})}};
 env.CLASSROOMS={idFromName:name=>name,get:id=>{
  if(!objects.has(id)){
   const storage=new Storage();let tail=Promise.resolve();
   const state={storage,blockConcurrencyWhile:fn=>{const run=tail.then(fn);tail=run.catch(()=>{});return run}};
   objects.set(id,new Classroom(state,env));
  }
  return objects.get(id);
 }};
 return {env,kv,objects};
}
async function call(env,path,body,token){
 const headers={'content-type':'application/json','cf-connecting-ip':'192.0.2.1'};
 if(token)headers.authorization='Bearer '+token;
 const response=await worker.fetch(new Request('https://class.test'+path,{method:body===undefined?'GET':'POST',headers,body:body===undefined?undefined:JSON.stringify(body)}),env);
 return {status:response.status,data:await response.json(),headers:response.headers};
}
const create=async env=>(await call(env,'/teacher/create',{})).data;
const join=async(env,code,name='Ученик')=>(await call(env,'/student/join',{code,name,consentVersion:1,consentConfirmed:true})).data;
const reports=(env,c)=>call(env,'/teacher/reports?classroomId='+c.classroomId,undefined,c.teacherToken);
const report=xp=>({format:'LinguaFreeStudyReport',version:1,student:'Ученик',completedLessons:1,xp,studyDaysLast7:1,studyDaysLast30:1,lastStudyDate:'2026-10-17',skills:Array.from({length:4},()=>({attempts:1,correct:1}))});
const sync=(env,s,revision,xp=10,snapshotId=String(revision).padStart(32,'0'))=>call(env,'/student/sync',{studentId:s.studentId,revision,snapshotId,report:report(xp)},s.studentToken);

test('health fails closed if a required binding is missing',async()=>{
 const {env}=environment();assert.equal((await call(env,'/health')).status,200);
 delete env.CLASSROOM_RATE_LIMITER;assert.equal((await call(env,'/health')).status,503);
});
test('classroom creation requires explicit operator enablement',async()=>{
 const {env}=environment();delete env.ALLOW_CLASSROOM_CREATION;
 assert.equal((await call(env,'/teacher/create',{})).status,403);
});
test('join requires explicit consent; roster includes learners before first report',async()=>{
 const {env}=environment(),c=await create(env);
 assert.equal((await call(env,'/student/join',{code:c.code,name:'A'})).status,400);
 const s=await join(env,c.code);const list=await reports(env,c);
 assert.equal(list.data.reports.length,1);assert.equal(list.data.reports[0].studentId,s.studentId);
 assert.equal(list.data.reports[0].pending,true);
});
test('teacher token cannot read a different classroom',async()=>{
 const {env}=environment(),a=await create(env),b=await create(env);
 assert.equal((await call(env,'/teacher/reports?classroomId='+b.classroomId,undefined,a.teacherToken)).status,401);
});
test('same names retain different student identities',async()=>{
 const {env}=environment(),c=await create(env),a=await join(env,c.code),b=await join(env,c.code);
 assert.notEqual(a.studentId,b.studentId);assert.equal((await reports(env,c)).data.reports.length,2);
 assert.equal((await call(env,'/student/sync',{studentId:b.studentId,revision:1,snapshotId:'0'.repeat(32),report:report(10)},a.studentToken)).status,401);
});
test('duplicate and delayed requests cannot overwrite a newer snapshot or its delta',async()=>{
 const {env}=environment(),c=await create(env),s=await join(env,c.code);
 await sync(env,s,1,10);await sync(env,s,3,30);
 assert.equal((await sync(env,s,3,30)).data.duplicate,true);
 assert.equal((await sync(env,s,2,20)).data.stale,true);
 const r=(await reports(env,c)).data.reports[0];assert.equal(r.report.xp,30);assert.equal(r.previousXp,10);
});
test('equal revisions with different identities are conflicts',async()=>{
 const {env}=environment(),c=await create(env),s=await join(env,c.code);
 await sync(env,s,1);assert.equal((await sync(env,s,1,10,'f'.repeat(32))).status,409);
});
test('concurrent newer and older snapshots end at the newer revision',async()=>{
 const {env}=environment(),c=await create(env),s=await join(env,c.code);
 await Promise.all([sync(env,s,4,40),sync(env,s,2,20),sync(env,s,3,30)]);
 assert.equal((await reports(env,c)).data.reports[0].report.xp,40);
});
test('revocation is retryable and prevents late sync from restoring data',async()=>{
 const {env}=environment(),c=await create(env),s=await join(env,c.code);await sync(env,s,1);
 const revoke=()=>call(env,'/student/revoke',{studentId:s.studentId},s.studentToken);
 await Promise.all([revoke(),sync(env,s,2)]);
 assert.equal((await revoke()).status,200);assert.equal((await sync(env,s,3)).status,401);
 assert.deepEqual((await reports(env,c)).data.reports,[]);
});
test('rotating invitations rejects old cached codes at the classroom authority',async()=>{
 const {env,kv}=environment(),c=await create(env);
 const rotated=await call(env,'/teacher/code',{classroomId:c.classroomId},c.teacherToken);
 kv.set('code:'+c.code,c.classroomId); // Simulate a stale KV edge cache.
 assert.equal((await call(env,'/student/join',{code:c.code,name:'A',consentVersion:1,consentConfirmed:true})).status,404);
 assert.ok((await join(env,rotated.data.code)).studentId);
});
test('teacher can revoke only a learner in their own class',async()=>{
 const {env}=environment(),c=await create(env),other=await create(env),s=await join(env,c.code);
 assert.equal((await call(env,'/teacher/remove',{classroomId:other.classroomId,studentId:s.studentId},other.teacherToken)).status,404);
 assert.equal((await call(env,'/teacher/remove',{classroomId:c.classroomId,studentId:s.studentId},c.teacherToken)).status,200);
 assert.equal((await sync(env,s,1)).status,401);
});
test('reports validate skill totals and reject arbitrary personal fields',async()=>{
 const {env}=environment(),c=await create(env),s=await join(env,c.code),r=report(10);
 r.skills[0].correct=9;
 assert.equal((await call(env,'/student/sync',{studentId:s.studentId,revision:1,snapshotId:'0'.repeat(32),report:r},s.studentToken)).status,400);
 r.skills[0].correct=1;r.secret='not stored';
 await call(env,'/student/sync',{studentId:s.studentId,revision:1,snapshotId:'0'.repeat(32),report:r},s.studentToken);
 assert.equal((await reports(env,c)).data.reports[0].report.secret,undefined);
});
test('rate limit returns a retry time and cannot be bypassed by anonymous bearer changes',async()=>{
 const {env}=environment();const keys=[];
 env.CLASSROOM_RATE_LIMITER.limit=async({key})=>{keys.push(key);return {success:false}};
 const r=await call(env,'/teacher/create',{},'arbitrary');await call(env,'/teacher/create',{},'another');
 assert.equal(r.status,429);assert.equal(r.headers.get('retry-after'),'60');assert.equal(keys[0],keys[1]);
});
test('oversized streamed payloads and non-HTTPS requests are rejected',async()=>{
 const {env}=environment();assert.equal((await call(env,'/teacher/create',{padding:'x'.repeat(12001)})).status,413);
 assert.equal((await worker.fetch(new Request('http://class.test/health'),env)).status,400);
});
test('inactive learning records expire after the retention period',async()=>{
 const {env,objects}=environment(),c=await create(env),s=await join(env,c.code),obj=objects.get(c.classroomId);
 const record=await obj.storage.get('student:'+s.studentId);record.lastActive=Date.now()-91*86400000;
 await obj.storage.put('student:'+s.studentId,record);await obj.alarm();
 assert.equal((await reports(env,c)).data.reports.length,0);
 assert.equal((await sync(env,s,1)).status,401);
});
