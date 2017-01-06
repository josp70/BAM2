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

read.bst = function( path ) {
  z1 = read.zoo(path, format="y%Ym%md%d", header=TRUE, sep=";")
  z1[,-ncol(z1)]
}

build.ts = function( z ) {
  t = ts(z)
  ts(t, frequency=findfrequency(t))
}

getDating = function(name) {
  switch(name,
    Yearly = Yearly,
    Quarterly = Quarterly,
    Monthly = Monthly,
    Weekly = Weekly,
    Daily = Daily)
}

Sread.bst = function(path) {
  table = read.csv2(path)
  dating = getDating(names(table)[1])
  firstD = as.Date(table[1,1], format="y%Ym%md%d")
  Serie(as.numeric(table[,2]), dating, firstD)
}

Swrite.bst = function(s, path) {
  data = data.frame(Sdates(s), as.numeric(s))
  names(data) = c(Sdating(s), "forecast")
  write.table(data, path, row.names=FALSE, quote=FALSE, sep=";")
}

z = Sread.bst(InputData)
y = as.ts(z)
fc = forecast(y)
lastD_obs = Slast(z)
firstD_fc = Dsucc(lastD_obs, Sdating(z))
z_fc = Serie(as.numeric(fc$mean), Sdating(z), firstD_fc)

Swrite.bst(z_fc, OutputData)

#st = file.copy(InputData, OutputData)

cat("BYE\n")
