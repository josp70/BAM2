package controllers

import java.io.{BufferedWriter, FileWriter}
import java.util.Locale
import javax.inject._

import akka.util.ByteString
import model.TimeSeries
import api.JsonCombinators._
import play.api.mvc.{Action, Controller, Results}

import scala.sys.process._
import play.api.Logger
import play.api.libs.json._

import scala.concurrent.Future

/**
  * Created by jorge on 22/12/16.
  */
class BamTables @Inject() (val currentEnv: play.api.Environment, val currentConf: play.api.Configuration) extends Controller {

  //var pathTolsh: String = "";
  var pathRscript: String = "";

  def getAppPath( name: String ): String = {
    val conf =  currentConf.getString(name)
    var value: String = ""
    if ( conf.isDefined ) {
      value = conf.get;
      val file = new java.io.File(value)
      if (file.exists()) {
        value = file.getAbsolutePath
      } else {
        value = ""
        Logger.error(s"Path ${name}=${value} does not exist")
      }
    } else {
      Logger.error(s"Path ${name} is not defined.")
    }
    return value
  }

  def ensureDir(path: String):Unit = {
    val pathDir = currentEnv.getFile (path)
    if (!pathDir.exists () ) {
      pathDir.mkdir ()
    }
  }

  def init():Unit = {
    //pathTolsh = getAppPath("path.tolapp")
    pathRscript = getAppPath("path.Rscript")
    // make sure the data directory structure is created
    ensureDir("data")
    ensureDir("data/uploaded")
    ensureDir("data/modelled")
    ensureDir("data/forecast")
    ensureDir("data/logs")
  }

  init()

  def getDirUploaded(): java.io.File = currentEnv.getFile("/data/uploaded")
  def getDirModelled(): java.io.File = currentEnv.getFile("/data/modelled")
  def getDirForecast(): java.io.File = currentEnv.getFile("/data/forecast")
  def getDirLogs(): java.io.File = currentEnv.getFile("/data/logs")
  def getDirTolResources(): java.io.File = currentEnv.getFile("/scripts/tol")
  def getDirRResources(): java.io.File = currentEnv.getFile("/scripts/R")

  def getFileUploaded(name: String): java.io.File = new java.io.File(getDirUploaded, name)
  def getFileModelled(name: String): java.io.File = new java.io.File(getDirModelled, name)
  def getFileForecast(name: String): java.io.File = new java.io.File(getDirForecast, name)
  def getFileLog(name: String): java.io.File = new java.io.File(getDirLogs, name)
  def getFileTol(name: String): java.io.File = new java.io.File(getDirTolResources, name)
  def getFileR(name: String): java.io.File = new java.io.File(getDirRResources, name)

  def getPathTolApp(): String = ""

  def postTimeSeries = Action(parse.json) { request =>
    if (pathRscript == "") {
      InternalServerError("unknown or invalid path to Rscript")
    } else {
      val methodForecast = request.getQueryString("method").getOrElse("forecast")
      val formatInput = request.getQueryString("inputOutput").getOrElse("json")
      val formatOutput = request.getQueryString("formatOutput").getOrElse("bdt")
      Logger.info("method = " + methodForecast)
      Logger.info("format = " + formatOutput)
      request.body.validate[TimeSeries] match {
        case JsSuccess(timeSeries, _) => {
          val id = java.util.UUID.randomUUID.toString
          val inputName = id + ".json"
          val outputName = id + ".bdt"
          var fileLog = id + ".log"
          val fileInput = getFileUploaded(inputName)
          val fileOutput = getFileForecast(outputName)
          val bufferedWriter = new BufferedWriter(new FileWriter(fileInput))
          //Json.toJson(timeSeries)
          bufferedWriter.write(Json.toJson(timeSeries).toString)
          bufferedWriter.close
          Logger.info("json written to " + fileInput)
          // invoke Rscript to forecast the data
          //http://www.scala-lang.org/api/rc2/scala/sys/process/ProcessBuilder.html
          val cmdRscript = Seq(pathRscript, "--vanilla", getFileR("process_data_json.r").getAbsolutePath,
            id, fileInput.getAbsolutePath, fileOutput.getAbsolutePath, formatInput, formatOutput, methodForecast) #> getFileLog(fileLog)
          cmdRscript.run
          Ok(id)
        }
        case JsError(errors) =>
          BadRequest(errors.toString())
      }
    }
  }

  val textOrJson = parse.using {
  request =>
    request.contentType.map(_.toLowerCase(Locale.ENGLISH)) match {
      case Some("application/json") | Some("text/json") => play.api.mvc.BodyParsers.parse.json
      case Some("text/plain") => play.api.mvc.BodyParsers.parse.text
      case _ => play.api.mvc.BodyParsers.parse.error(Future.successful(UnsupportedMediaType("Invalid content type specified")))
    }
  }

  def echoTxtOrJson = Action(textOrJson) {
    request =>
      request.body match {
        case json: JsObject => Ok(json) //echo back posted json
        case text: String => Ok(text) //echo back posted XML
      }
  }

  def postForecast = Action(textOrJson) { request =>
    if (pathRscript == "") {
      InternalServerError("unknown or invalid path to Rscript")
    } else {
      val methodForecast = request.getQueryString("method").getOrElse("forecast")
      val formatOutput = request.getQueryString("formatOutput").getOrElse("bdt")
      Logger.info("method = " + methodForecast)
      Logger.info("format = " + formatOutput)
      val id = java.util.UUID.randomUUID.toString
      val nameOutput = id
      val nameLog = id + ".log"
      val nameInput = id
      val (formatInput:String, bufferInput:String) = request.body match {
        case json: JsObject => json.validate[TimeSeries] match {
          case JsSuccess(timeSeries, _) => {
            ("json", Json.toJson(timeSeries).toString)
          }
          case JsError(errors) =>
            BadRequest(errors.toString())
        }
        case text: String => {
          ("bdt", text)
        }
      }
      val fileInput = getFileUploaded(nameInput)
      val fileOutput = getFileForecast(nameOutput)
      val bufferedWriter = new BufferedWriter(new FileWriter(fileInput))
      //Json.toJson(timeSeries)
      bufferedWriter.write(bufferInput)
      bufferedWriter.close
      Logger.info("input data written to " + fileInput)
      // invoke Rscript to forecast the data
      //http://www.scala-lang.org/api/rc2/scala/sys/process/ProcessBuilder.html
      val cmdRscript = Seq(pathRscript, "--vanilla", getFileR("process_data.r").getAbsolutePath,
        id, fileInput.getAbsolutePath, fileOutput.getAbsolutePath, formatInput, formatOutput, methodForecast) #> getFileLog(nameLog)
      cmdRscript.run
      Ok(id)
    }
  }

  def postForecastOff = Action(parse.multipartFormData) { request =>
    if (pathRscript == "") {
      InternalServerError("unknown or invalid path to Rscript")
    } else {
      val methodForecast = request.getQueryString("method").getOrElse("forecast")
      val formatInput = request.getQueryString("formatInput").getOrElse("bdt")
      val formatOutput = request.getQueryString("formatOutput").getOrElse("bdt")
      Logger.info("method = " + methodForecast)
      if (formatInput == "bdt") {
        request.body.file("dataframe").map { file =>
          Logger.info(s"Received: ${file.filename}")
          val id = java.util.UUID.randomUUID.toString
          val inputName = id + ".bdt"
          val outputName = id + ".bdt"
          var fileLog = id + ".log"
          val fileInput = getFileUploaded(inputName)
          val fileOutput = getFileForecast(outputName)
          file.ref.moveTo(fileInput)
          Logger.info("dataframe copied to " + fileInput.getPath)
          // invoke Rscript to forecast the data
          //http://www.scala-lang.org/api/rc2/scala/sys/process/ProcessBuilder.html
          val cmd = Seq(pathRscript, "--vanilla", getFileR("process_data.r").getAbsolutePath,
            id, fileInput.getAbsolutePath, fileOutput.getAbsolutePath, formatInput, formatOutput, methodForecast) #> getFileLog(fileLog)
          cmd.run
          Ok(id)
        }.getOrElse {
          BadRequest("dataframe is missing")
        }
      } else {
        BadRequest(s"format=${formatInput} invalid. Valid formats are: bdt")
      }
    }
  }

 def getForecast(id: String) = Action {
    val outputName = id
    val fileOutput = getFileForecast(outputName)
    if (fileOutput.exists()) {
      // https://www.playframework.com/documentation/2.5.x/ScalaStream
      Ok.sendFile(
        fileOutput,
        inline=false).withHeaders(CACHE_CONTROL->"max-age=3600",
                                  CONTENT_DISPOSITION -> ("attachment; filename="+outputName))
    } else {
      NoContent
    }
  }

  def downloadLog(id: String) = Action {
    val outputName = id + ".log"
    val fileOutput = getFileLog(outputName)
    if (fileOutput.exists()) {
      // https://www.playframework.com/documentation/2.5.x/ScalaStream
      Ok.sendFile(fileOutput,
        inline=false).withHeaders(CACHE_CONTROL->"max-age=3600",
        CONTENT_DISPOSITION -> ("attachment; filename="+outputName))
    } else {
      NoContent
    }
  }
}
