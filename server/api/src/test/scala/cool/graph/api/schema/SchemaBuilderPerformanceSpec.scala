package cool.graph.api.schema

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.api.ApiBaseSpec
import cool.graph.shared.models.{Project, ProjectWithClientId}
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json
import sangria.schema.{InputObjectType, Schema}

import scala.collection.mutable
import scala.reflect.io.File

class SchemaBuilderPerformanceSpec extends FlatSpec with Matchers with ApiBaseSpec {

  val schemaBuilder = SchemaBuilder()

  def writeSchemaIntoFile(schema: String): Unit = File("schema").writeAll(schema)

  "schema" should "be measured" in {
    measureLoop("big_schema", iterations = 100)
  }

  def measureLoop(projectId: String, iterations: Int) = {
    println(s"MEASURING $projectId")

    val (readJsonTime, jsonString) = readJsonFromDisk(projectId)
    println(s"read json (${jsonString.length} chars) in ${readJsonTime}ms")

    val (deserializeTime, project) = deserializeProject(jsonString)
    println(s"parsed json in ${deserializeTime}ms")
    println(s"""the project has:
         | fields: ${project.schema.models.flatMap(_.fields).size}
         | relations: ${project.relations.size}
       """.stripMargin)

    val buildTimes = mutable.Buffer.empty[Long]
    for (_ <- 1 to iterations) yield {
      val buildTime = buildTimed(schemaBuilder(project))
      buildTimes += buildTime
    }
    println(s"build time MIN: ${buildTimes.min}")
    println(s"build time MAX: ${buildTimes.max}")
    println(s"build time AVG: ${buildTimes.sum / buildTimes.size}")
  }

  def deserializeProject(schema: String): (Long, Project) = timedAndResult {
    import cool.graph.shared.models.ProjectJsonFormatter._
    Json.parse(schema).as[ProjectWithClientId].project
  }

  def readJsonFromDisk(projectId: String): (Long, String) = timedAndResult {
    val stream     = getClass.getResourceAsStream(s"/$projectId.json")
    val jsonString = scala.io.Source.fromInputStream(stream).getLines.mkString
    stream.close()
    jsonString
  }

  def buildTimed(schemaBuilder: => Schema[ApiUserContext, Unit]): Long = timed {
    build(schemaBuilder)
  }

  def build(schemaBuilder: => Schema[ApiUserContext, Unit]): Unit = {
    val schema           = schemaBuilder
    val inputObjectTypes = schema.inputTypes collect { case (name, tpe: InputObjectType[_]) => tpe }
    inputObjectTypes.foreach(_.fields)
  }

  def timed[T](block: => T): Long = {
    val start = System.currentTimeMillis()
    block
    System.currentTimeMillis() - start
  }

  def timedAndResult[T](block: => T): (Long, T) = {
    val start   = System.currentTimeMillis()
    val result  = block
    val elapsed = System.currentTimeMillis() - start
    (elapsed, result)
  }
}
/*

ProjectId                 Fields

cj51o1wzbb5cj01759gsvsd1l	929
cj0s4ue7wk1f90102dcal8gd5	900
cj215649nd1bj01646f00vq2f	900
ciwyoxmas3amt0129hec6zpvl	581
cj2u073h3z0gk01829ukqdgb1	508
cj5kcbptijcei01344l4yhl20	490
cj50a9abehou101965eehb36d	471
cj50873cbfeh70175p6sg41ji	465
citud9y3j061z0105dwh69ue2	421
cj0pz4wqqw7de01024kq1gq81	386

cj4xq5y3mlm8b0175o0gezwwa	385
cj0mmmtoyv9kl010236qhsk5d	382
cj0mak2n1ga3d0118sp5wtlw1	381
cj41zvinz24vm01235pepodjv	365
cj5lcb3pci6zi0122ctbo9g7w	356
cj03cjwk3c2tk0122m9hdro9q	347
cj37akeyl6pyc01038p2kdmks	325
cj0ml2vsis67m0118qvx1nt5j	314
cj0ml5dbksdky010232knzil8	314
cj0ml1rhfs6ck0102n0qjw8u7	314
cj0ml74cxse1y0118inlma3e0	314
cj4q7d8bpqcfe0169yl5w3lud	296
cj4gzqwyzcapv01570zyik3jr	294
cj4b19yu00p9n014244ck1sg0	289
cj51m2ngh98180175w0nfki00	283
cj4qpbxwwzy6s0196b5ev5qqd	281
cj41xnuyfqrda01859wcovihw	276
cj596yktcokoa0105mqlkyy3j	274
cj2m6t1xa7xt601759clap65p	273
cj32ti8u8khzz0122jd4cwzh6	273
cj5wh6xhd0h06012745y00fs1	272
cj0whv7ioi40c0102wo7xk7mp	271
cj57v8algl0py0181lgmarwwx	271
cj1wi06k87jo60137aqfrrjat	267
cj23kouvvjmku0101bu87pbhl	266
cj5ia99qi4aic01220xvtiw6x	266
cizjfivcnbxe001722kth9npv	263
cj58yedcybfzv01050lhcav80	261
cj15ypbi98bsw0118rqgk3vsc	259
cj4o8dmkta0jf0118gl9qlaz7	258
cj4a0krgso3zo01731w4r29ja	257
cj5xhqk2m7yj80145fvph6t7o	246
cj0ybo0br0ns20115ytdjsyqc	246
cj4fuvpm2j3b30157b0ol2z5x	245
cj1g3xqbdsi5j0171yji1p8j0	243
cj21w1r7krkpj01015q43znuv	242
cj5ku2xwrc6pc0134x3k7g09b	240
ciuy0184g0w450159clwbufk5	240
cj41c9u2zddol0177la66g30g	238
cj0mjs09xq37r0102ffmyg1aa	238
cislqwo6e039e01171nzb9mxw	235
cj2p0bpgli5a501757pns1w90	225
cj47d1w08g20k0121oq9cq1q5	225
cj5jl0vhz5vop0122m3hcz7gy	224
cj3vjpn6jar770194uv3pvm2f	224
cj1s0ystj9rnf01048fueoedr	221
cj2z0761fwadh011047neqmax	221
cj2rnb8m3gsrr0123xnf1m9r5	220
cj2m0zbzt2dj40175m39qhftb	220
cj5ig5ly19yom0127msq048g9	220
cj15aml450h2n0187dqu93i7u	217
cj2zmcnc5f70s017668iyv1my	215
cj2zmhsvzf5bt0190o5hupk49	215
cix0wl6br05ol0132l1ojm13v	215
cj2zm9ipef2li0190dkyyb26o	215
ciz7rke1h6l8n0123z6sftahd	212
cj5dokuw3dtsd0122iv017mwk	211
cj4teh96wfx5k0185ffuum4jk	210
cj3lda03fawb00160alp2jufy	209
cizao5xe14rfp0166d827jbmn	209
cj0ti9z0e0zx301024ac4yot1	203
cj181bsf22crn0189q8au5ijj	201
cj0pzrv95wghh0102w3xc4e5j	201
cj5gl02rh857s0122xb3w8ra4	200
ciyx06u900lk8016093sfx201	199
cj523uz7osai10175ja9p8dsh	199
cj1sa7404i7rk01048fnquf0t	198
cixlg7sc818wu01777l47joao	198
cj5nbkzhsejyd0122xngrnr17	198
cj2gwg6txfr2b0107123bxxky	197
cj05lj90u0ju30144z4actoyy	194
cj48sbq259f4d0141or2p4yq5	193
civil5oma0ncc0130uvicmchh	192
citwpbyyr0dix0122tk1qbbs3	191
cj0p7sv6jhuab0118opm04fb5	191
cj379nsb35yh40103ywvgezw1	188
cj21yqfrrsuvm0164ks76umjm	188
cj1poeo1h0ru201619jyad6js	188
cj50qgri9tsvm0175f1p28fan	187
cisskegu251wc0117lq09w8b3	185
ciwp3lmzo0d2h01251utuwaas	185
cj13l86t1d6wv0155ux63bzcb	185
cj537nvidjcen01750yom8f88	185
cj5v4nebateeq0123ai9vewks	185
cj4yec3zlgg6e0196f9xqlsjc	184
citwh70aj07gi01222082camo	184
cj5kr0bde66160134ppmbojic	184
cj5c8sqec0nx50108c5rflwz9	183
ciypuk8134jmq0120uspx3ewq	182
cj3x8bw9m7iof016339xh9oio	182
cj3fzjzv358yp0111okiy98sw	182
cj4iwj8or3s1g01923yozvaul	182
cj5c400hu2x470139l52zo92g	179
cizg3qgpl2q1l0179u5gjo5p2	179
cj2om6hf09p5r0175jl9ppexg	179
cj41och7kk9o20177z31wf0fp	178
cizr8qzlojf0e0127qlhjpyls	177
cj5b199r2mbin01138rtws33t	176
cj45fwb23e4wl012682yhzkwv	176
cj01olu0fxptu0143llqu3f3r	175
cj5slcu5ufdnz0160n9huaxny	174
cj2rrwtpjql4k016026r7rtdv	174
cj5cj61pgi1r50127lduca1t6	172
cj48ialbhrylt0173jxr8joor	172
cj5c7eflt7qq50139a84zh1wf	172
cj0lepdeeeoqz0139z60ecerf	172
cj5cj36dihz2j0108eg0srn89	172
cj2xb9gd1izf80154kdidk19q	172
cj53wkdjw9qfl0175056wwyjk	172
ciyob6fnu3qjl01329outsfgk	171
cj5hum6zpiq7s0122rglymwal	171
ciyhbuuno0dgj0120utmv5fjv	170
cj0o28ynezelo0102u8w7tm3x	169
cj53o76tazsmm01968fhkemhs	168
cj2x830ytg0qy0194sce9b91a	168
cj12j7193vwj20138ilapopxw	168
ciyxphjhp0wz20114jf8kgea8	168
cj49msi078fc20173z65rxjgl	167
cj1lrvkcs9twe01781k2g2co4	167
ciulahmih0szk0156dkc5dh9q	166
civgp3afi01ft0184gayipxf3	166
cj2sxyd207al80160gpet46qt	166
cj2hvatqyd6f501223lcnx97g	165
cj292ed8v9gja0199ggwv9lwr	165
cixraxev60e4c0121krsia44h	163
cizza3dqfnjra0133o1p0iq1l	162
cixcqgrex44k20111wo4ete7z	162
cj1w9hesu3gdq01934crc1b23	161
cj5apzf6x0egk0105n9g0imcz	161
cj3pjmayo8j270174gb8ssk7i	161
cizl2oa7zajxa0108bl5xuu2f	161
cj24jwmf57uvo0164y2bas64h	160
cj4f11fdzkssf01927snsy3l1	160
cj5cqxs29xnmh0127oy28rn68	159
cj11r9icf46nu0145g3aglzqt	158
cj1zqky1z9rrq0100pvkrinqc	158
cj2u8k5eph5fy0179600f50ru	157
cj2th95cksrrx0123gzt3z3jt	157
cj4aet1qr07e70141ymb209y9	157
cj3lhtf6112pc0156blxth2cf	155
cj2okidx98zdb0175abw32k91	155
ciwvt2dft141u0129v0m5rt21	155
cj1pjdvk3jhui01703ft8ixwb	155
cj1oavyelqw030199k1wk9hjy	155
cj1picaq0iulp0146ttmr1mum	155
cj5fde1nfxoaa0127t61b1181	155
ciz1sg60wu59a0185znnjecol	153
cj3706mp61pvz0103l5kx171f	152
cj4skjweae56i0185bb3zg5tc	152
ciy5rgnkd0n150170exu2svzo	151
cj3ng0rr5hosw0189k4nw2yue	151
cj3oz3ikh78sf01428xxm57nh	151
ciwrww4qv35jt0125emypitjt	150
cj3obxt2a5i300130tvw1dz9k	150
cj1lb7pw83l470178mpmk61qv	150
cj47629pv8mq101210r4p9wyj	150
cj0jrko17ie1201332kggfv1m	150
cj578b5lwgwwt0118hl6l88b2	149
cj1xbi2rukym10133a0achwfn	149
cj4ekacx79vxc0124danbainf	149
cj3bmtukgjfny0147phe0ik3i	146
cj3gcpvck8emf0162hwfdf5ji	146
cj5dlz64h9mx80122bt8zbnk3	146
ciy5z6scz028301951epuuled	145
ciyo5umwf3o570120k3u30ir1	145
ciyo6jb2f3o7k01323jht40p7	145
cj3oeaeg97mi50130i8tzk6az	144
cj5w8u9kp04ji0127gufbvij9	144

 */
