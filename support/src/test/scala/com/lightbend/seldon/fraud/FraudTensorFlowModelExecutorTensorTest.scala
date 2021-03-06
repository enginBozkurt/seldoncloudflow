package com.lightbend.seldon.fraud

import com.lightbend.seldon.executors.tensor.TensorFlowModelExecutorTensor
import org.scalatest.FlatSpec
import tensorflow.modelserving.avro._
import tensorflow.support.avro._

class FraudTensorFlowModelExecutorTensorTest extends FlatSpec {

  val descriptor = ModelDescriptor(
    modelName = "Fraud model",
    description = "Fraud tensorflow saved model",
    modelSourceLocation = None,
    bucket = None
  )

  val localDirectory = "/Users/boris/Projects/TFGRPC/data/fraud/model/1"

  val transactions = Seq(1.22965763450793,0.141003507049326,0.0453707735899449,1.20261273673594,0.191880988597645,0.272708122899098,-0.00515900288250983,0.0812129398830894,0.464959994783886,-0.0992543211289237,-1.41690724314928,-0.153825826253651,-0.75106271556262,0.16737196252175,0.0501435942254188,-0.443586797916727,0.00282051247234708,-0.61198733994012,-0.0455750446637976,-0.21963255278686,-0.167716265815783,-0.270709726172363,-0.154103786809305,-0.780055415004671,0.75013693580659,-0.257236845917139,0.0345074297438413,0.00516776890624916,4.99)
  val dtype = DataType.DT_FLOAT
  val shape = TensorShape(Seq(Dim(1L, ""), Dim(transactions.size.toLong, "")))
  val tTensor = Tensor(dtype = dtype, tensorshape = shape, float_data = Some(transactions.map(_.toFloat)))
  val pTensor = Tensor(dtype = dtype, tensorshape = shape)


  "Processing of model" should "complete successfully" in {

    val executor = new TensorFlowModelExecutorTensor(descriptor, localDirectory)
    println("Executor created")
    val result = executor.score(SourceRequest(inputRecords = SourceRecord(Map("transaction" -> tTensor)),
      modelResults = ServingOutput(Map("predictions" -> pTensor))))
    println(result)
  }
}