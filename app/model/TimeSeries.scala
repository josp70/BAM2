package model
import org.joda.time.DateTime

/**
  * Created by jorge on 11/01/17.
  */

case class SeriesData(name: String, first: DateTime, values: Seq[Option[Double]])
case class TimeSeries(timeset: String, dates: Seq[DateTime], series: Seq[SeriesData])
