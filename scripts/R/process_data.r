#!/usr/bin/env Rscript
args = commandArgs(trailingOnly=TRUE)

# test if there are 3 arguments: if not, return an error
if (length(args)!=3) {
  stop("Three arguments must be provided id input.data output.model", call.=FALSE)
}

ID_Data = args[1]
InputData = args[2]
OutputData = args[3]

cat("ID_Data =", ID_Data, "\n")
cat("InputData =", InputData, "\n")
cat("OutputData =", OutputData, "\n")

st = file.copy(InputData, OutputData)

cat("BYE\n")
