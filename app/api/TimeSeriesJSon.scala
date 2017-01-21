package api

import model._
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import org.joda.time.DateTime

import play.api.libs.json._

/**
  * Created by jorge on 11/01/17.
  */

object JsonCombinators {

  var dateFormat = "epoch"

  implicit object OptDoubleReads extends Reads[Option[Double]] {
    def reads(json: JsValue) = json match {
      case JsNull =>  JsSuccess(None)
      case JsNumber(n) => JsSuccess(Some(n.toDouble))
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsnumber.or.null"))))
    }
  }

  //implicit val dateWrites = Writes.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  //implicit val dateReads = Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

  //implicit val dateWrites = Writes.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  //implicit val dateReads = json.Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

  /*
  implicit object readSeriesData extends Reads[SeriesData] {
    def reads(json: JsValue): JsResult[SeriesData] = {
      try {
        val name = (json \ "name").as[String]
        val first = (json \ "first").as[String]
        val values = (json \ "values").as[Seq[Option[Double]]]
        println("first =" + first)
        JsSuccess(SeriesData(name, new DateTime(first), values))
      } catch {
        case e: Exception => {
          JsError(e.getMessage)
        }
      }
    }
  }

  implicit object readTimeSeries extends Reads[TimeSeries] {
    def reads(json: JsValue): JsResult[TimeSeries] = {
      try {
        val dating = (json \ "dating").as[String]
        val dates = (json \ "dates").as[Seq[String]]
        val series = (json \ "series").as[Seq[SeriesData]]
        JsSuccess(TimeSeries(dating, dates.map(new DateTime(_)), series))
      } catch {
        case e: Exception => {
          JsError(e.getMessage)
        }
      }
    }
  }
  */
  implicit val readSeriesData = Json.reads[SeriesData]

  /*
  implicit object readSeriesData extends json.Reads[SeriesData] {
    def reads(json: JsValue): JsResult[SeriesData] = {
      val name = (json \ "name").get.toString
      val first = new DateTime((json \ "first").get.toString)
      val values = (json \ "values").as[Seq[Option[Double]]]
      JsSuccess(SeriesData(name, first, values))
    }
  }

  val json1 = Json.parse("""{"fecha": "2017-01-01T00:00:00.000+0100"}""")
  val json2 = Json.parse("""{"fecha": "2017-01-01"}""")
  case class Datos(fecha: DateTime)

  implicit object readDatos extends Reads[Datos] {
    def reads(json: JsValue): JsResult[Datos] = {
      val fecha = (json \ "fecha").as[String]
      JsSuccess(Datos(new DateTime(fecha)))
    }
  }
  json1.as[Datos]
  */

  /*
  implicit val readSeriesData: Reads[SeriesData] = (
    (__ \ "name").read[String] and
      (__ \ "values").read[Seq[Option[Double]]]
    ) (SeriesData.apply _)
*/

  implicit val writeSeriesData = Json.writes[SeriesData]

  /*
  implicit val writeSeriesData: Writes[SeriesData] = (
    (__ \ "name").write[String] and
      (__ \ "values").write[Seq[Option[Double]]]
    ) (unlift(SeriesData.unapply))
*/

  implicit val readTimeSeries = Json.reads[TimeSeries]

  /*
  implicit val readTimeSeries: Reads[TimeSeries] = (
    (__ \ "dating").read[String] and
      (__ \ "dates").read[Seq[String]] and
      (__ \ "series").read[Seq[SeriesData]]
    ) (TimeSeries.apply _)
*/

  implicit val writeTimeSeries =  Json.writes[TimeSeries]
  /*
  implicit val writeTimeSeries: Writes[TimeSeries] = (
    (__ \ "dating").write[String] and
      (__ \ "dates").write[Seq[String]] and
      (__ \ "series").write[Seq[SeriesData]]
    ) (unlift(TimeSeries.unapply))
    */

}