package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

//import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.core._
//import com.github.plokhotnyuk.jsoniter_scala.macros.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
//import com.github.plokhotnyuk.jsoniter_scala.macros.UPickleReaderWriters._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
//import upickle.default._

case class NestedStructs(n: Option[NestedStructs])

class NestedStructsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 128
  var obj: NestedStructs = _
  var jsonBytes: Array[Byte] = _
  var jsonString: String = _
  var preallocatedOff: Int = 128
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 to size).foldLeft(NestedStructs(None))((n, _) => NestedStructs(Some(n)))
    jsonBytes = writeToArray(obj)(nestedStructsCodec)
    jsonString = new String(jsonBytes, UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + preallocatedOff + 100/*to avoid possible out of bounds error*/)
  }
/* FIXME: AVSystem GenCodec cannot parse option values when field is missing
  @Benchmark
  def readAVSystemGenCodec(): NestedStructs = JsonStringInput.read[NestedStructs](new String(jsonBytes, UTF_8))
*/
  @Benchmark
  def readCirce(): NestedStructs = decode[NestedStructs](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): NestedStructs = jacksonMapper.readValue[NestedStructs](jsonBytes)

  @Benchmark
  def readJsoniterScala(): NestedStructs = readFromArray[NestedStructs](jsonBytes)(nestedStructsCodec)

  @Benchmark
  def readPlayJson(): NestedStructs = Json.parse(jsonBytes).as[NestedStructs](nestedStructsFormat)

/* FIXME: cannot alter uPickle to parse missing optional fields as None
  @Benchmark
  def readUPickle(): NestedStructs = read[NestedStructs](jsonBytes)
*/
/* FIXME: AVSystem GenCodec serializes option values field with null value
  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)
*/
  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)(nestedStructsCodec)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int =
    writeToPreallocatedArray(obj, preallocatedBuf, preallocatedOff)(nestedStructsCodec)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(nestedStructsFormat))
/* FIXME: uPickle serializes empty optional values
  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
*/
}