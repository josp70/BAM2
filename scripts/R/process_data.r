#!/usr/bin/env Rscript
args = commandArgs(trailingOnly=TRUE)

# test if there are 3 arguments: if not, return an error
if (length(args)!=5) {
  stop("five arguments must be provided: <id> <input.data> <output.model> format method", call.=FALSE)
}

library(forecast)
library(tolBasis)

ID_Data = args[1]
InputData = args[2]
OutputData = args[3]
InputFormat = args[4]
MethodForecast = args[5]

cat("ID_Data =", ID_Data, "\n")
cat("InputData =", InputData, "\n")
cat("OutputData =", OutputData, "\n")
cat("InputFormat =", InputFormat, "\n")
cat("MethodForecast =", MethodForecast, "\n")

#head(read.csv2(InputData))

build.ts = function( z ) {
  t = ts(z)
  ts(t, frequency=findfrequency(t))
}

getDating = function(name) {
  eval(parse(text=name))
}

Sread.bdt = function(path) {
  table = read.table(path, header=TRUE, sep=";")
  #print(head(table))
  dating = getDating(names(table)[1])
  firstD = as.Date(table[1,1], format="y%Ym%md%d")
  #print(head(table[,2]))
  #print(head(as.numeric(table[,2])))
  Serie(table[,2], dating, firstD)
}

Swrite.bdt = function(s, path) {
  dating = Sdating(s)
  # Sdates tiene un error que se salta la primera fecha, ojo incluso con el -2, -1 no da el anterior
  # seqDates = Dseq(Dsucc(Sfirst(s), dating, -2), , dating, length(s))
  data = data.frame(Sdates(s), as.numeric(s))
  names(data) = c(Sdating(s), "forecast")
  write.table(data, path, row.names=FALSE, quote=FALSE, sep=";")
}

z = Sread.bdt(InputData)
y = as.ts(z)
fc = forecast(y)
lastD_obs = Slast(z)
firstD_fc = Dsucc(lastD_obs, Sdating(z))
#print(c(lastD_obs, firstD_fc))
z_fc = Serie(as.numeric(fc$mean), Sdating(z), firstD_fc)


Swrite.bdt(z_fc, OutputData)
#write.csv2(as.numeric(z), "/tmp/Z.csv")
#write.csv2(as.numeric(z_fc), OutputData)

#st = file.copy(InputData, OutputData)

cat("BYE\n")
