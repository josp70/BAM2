package controllers

import javax.inject._

import akka.util.ByteString
import play.api.mvc.{Action, Controller, Results}

import scala.sys.process._
import play.api.Logger

/**
  * Created by jorge on 22/12/16.
  */
class BamTables @Inject() (val currentEnv: play.api.Environment, val currentConf: play.api.Configuration) extends Controller {

  var pathTolsh: String = "";
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
    pathTolsh = getAppPath("path.tolapp")
    pathRscript = getAppPath("pathRscript")
    // make sure the data directory structure is created
    ensureDir("data")
    ensureDir("data/uploaded")
    ensureDir("data/modelled")
    ensureDir("data/logs")
  }

  init()

  def getDirUploaded(): java.io.File = currentEnv.getFile("data/uploaded")
  def getDirModelled(): java.io.File = currentEnv.getFile("data/modelled")
  def getDirLogs(): java.io.File = currentEnv.getFile("data/logs")
  def getDirTolResources(): java.io.File = currentEnv.getFile("scripts/tol")
  def getDirRResources(): java.io.File = currentEnv.getFile("scripts/R")

  def getFileUploaded(name: String): java.io.File = new java.io.File(getDirUploaded, name)
  def getFileModelled(name: String): java.io.File = new java.io.File(getDirModelled, name)
  def getFileLog(name: String): java.io.File = new java.io.File(getDirLogs, name)
  def getFileTol(name: String): java.io.File = new java.io.File(getDirTolResources, name)
  def getFileR(name: String): java.io.File = new java.io.File(getDirRResources, name)

  def getPathTolApp(): String = ""

  def upload1 = Action(parse.multipartFormData) { request =>
    request.body.file("dataframe").map { file =>
      Ok(s"File uploaded: ${file.filename}")
    }.getOrElse {
      BadRequest("File is missing")
    }
  }

  def upload = Action(parse.multipartFormData) { request =>
    if (pathTolsh == "") {
      InternalServerError("unknown or invalid path to tolsh")
    } else {
      request.headers.get("lang")
      request.body.file("dataframe").map { file =>
        Logger.info(s"Received: ${file.filename}")
        val id = java.util.UUID.randomUUID.toString
        val inputName = id + ".data"
        val outputName = id + ".model"
        var fileLog = id + ".log"
        val fileInput = getFileUploaded(inputName)
        val fileOutput = getFileModelled(outputName)
        file.ref.moveTo(fileInput)
        Logger.info("dataframe copied to " + fileInput.getPath)
        // invoke TOL to model the data
        //http://www.scala-lang.org/api/rc2/scala/sys/process/ProcessBuilder.html
        val cmd = Seq(pathTolsh,
          """-cText ID_Data=\"""" + id + """\"""",
          """-cText InputData=\"""" + fileInput.getAbsolutePath + """\"""",
          """-cText OutputData=\"""" + fileOutput.getAbsolutePath + """\"""",
          getFileTol("process_data.tol").getAbsolutePath) #> getFileLog(fileLog)
        cmd.run
        Ok(id)
      }.getOrElse {
        BadRequest("dataframe is missing")
      }
    }
  }

  def upload0 = Action(parse.temporaryFile) { request =>
    if (pathTolsh == "") {
      InternalServerError("unknown or invalid path to tolsh")
    } else {
      val id = java.util.UUID.randomUUID.toString
      val inputName = id + ".data"
      val outputName = id + ".model"
      var fileLog = id + ".log"
      val fileInput = getFileUploaded(inputName)
      val fileOutput = getFileModelled(outputName)
      request.body.moveTo(fileInput)
      // invoke TOL to model the data
      //http://www.scala-lang.org/api/rc2/scala/sys/process/ProcessBuilder.html
      val cmd = Seq(pathTolsh,
        "-cText ID_Data=\"" + id + "\"",
        "-cText InputData=\"" + fileInput.getAbsolutePath + "\"",
        "-cText OutputData=\"" + fileOutput.getAbsolutePath + "\"",
        getFileTol("process_data.tol").getAbsolutePath) #> getFileLog(fileLog)
      cmd.run
      Ok(id)
    }

  }

 def download(id: String) = Action {
    val outputName = id + ".model"
    val fileOutput = getFileModelled(outputName)
    if (fileOutput.exists()) {
      // https://www.playframework.com/documentation/2.5.x/ScalaStream
      Ok.sendFile(
        fileOutput,
        inline=false).withHeaders(CACHE_CONTROL->"max-age=3600",
                                  CONTENT_DISPOSITION -> ("attachment; filename="+outputName))
    } else {
      Ok("File still not created")
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
      Ok("File still not created")
    }
  }
}
