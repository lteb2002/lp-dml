package com.reremouse.dml.solver.optimizer.admm.lp

import breeze.linalg.{DenseMatrix, DenseVector, inv, norm, pinv, sum}
import breeze.numerics.log
import breeze.optimize.{DiffFunction, LBFGS}
import com.reremouse.dml.solver.optimizer.admm.AdmmUpdater
import com.reremouse.dml.solver.optimizer.test.AdmmLpSolverBfSumtTest
import com.reremouse.dml.solver.optimizer.{DFP, MathFunction, Momentum, SteepestDescent}

class X1UpdaterLpBf extends AdmmUpdater {

  var c1: DenseVector[Double] = null
  var c2: DenseVector[Double] = null
  var A1: DenseMatrix[Double] = null
  var A2: DenseMatrix[Double] = null
  var b: DenseVector[Double] = null

  var A1TA1: DenseMatrix[Double] = null
  //var A1TA1_1: DenseMatrix[Double] = null
  var A1TA2: DenseMatrix[Double] = null
  var A1t: DenseMatrix[Double] = null

  def this(c10: DenseVector[Double],
           c20: DenseVector[Double],
           A10: DenseMatrix[Double],
           A20: DenseMatrix[Double],
           b0: DenseVector[Double]) {
    this()
    this.c1 = c10
    this.c2 = c20
    this.A1 = A10
    this.A2 = A20
    this.b = b0
    A1TA1 = A1.t * A1
//    try {
//      A1TA1_1 = inv(A1TA1)
//    } catch {
//      case ex: Exception => {
//        println("pinv")
//        A1TA1_1 = pinv(A1TA1)
//      }
//    }
    A1TA2 = A1.t * A2
    A1t=A1.t
  }


  /**
    * 输入参数，输出更新后的值
    *
    * @param x1
    * @param x2
    * @param lambda
    * @param beta
    * @return
    *
    */
  override def update(x1: DenseVector[Double],
                      x2: DenseVector[Double],
                      lambda: DenseVector[Double],
                      beta: Double=2,name:String="X1 updater",
                      regType: String = "l2",
                      regWeight: Double=1): DenseVector[Double] = {
    //val xx = A1TA1_1 * (((A1.t * lambda) / beta) - (c1 / beta) - A1TA2 * partB +  A1.t*b)

    val df = new MathFunction() {
      override def computeObjective(x: DenseVector[Double],args:Array[Any]=Array[Any]()): Double = {
        val reg = sum(x *:* x) * sum(x *:* x) + sum(x2 *:* x2) * sum(x2 *:* x2)
        c1.t*x  +c2.t*x2- sum(AdmmLpSolverBfSumtTest.u*log(x))- sum(AdmmLpSolverBfSumtTest.u*log(x2)) - lambda.t * (A1*x+A2*x2-b) + (beta/2)*(Math.pow(norm(A1*x+A2*x2-b),2))+ regWeight * reg
        //c1.t*(x*:*x) +c2.t * (x2*:*x2)  - lambda.t * (A1*(x*:*x)+A2*(x2*:*x2)-b) + (beta/2)*norm(A1*(x*:*x)+A2*(x2*:*x2)-b)
      }
      override def computeGradient(x: DenseVector[Double],args:Array[Any]=Array[Any]()): DenseVector[Double] = {
        //println("uu:"+uu)
        val reg = sum(x *:* x) * x * 4.0
        val dfx = c1 - AdmmLpSolverBfSumtTest.u/x -A1.t*lambda + beta*A1t *(A1*x + A2*x2 - b)+ regWeight * reg
        //val dfx = 2.0*(c1*:*x)  - (2.0*A1.t * lambda)*:*x + (2.0*beta*A1.t *(A1*(x*:*x) + A2*(x2*:*x2) - b)*:*x)
        dfx
      }
    }

    var xx: DenseVector[Double] = x1
    var error = 1.0
    var step = 0
    val maxStep = 100000000

//    val method = new SteepestDescent(stepSize = 0.001,
//      ifFixedStep = true,
//      ifPositiveValue = true,
//      precision = 1.0E-2)//ADMM的子迭代中，可以取较低精度，这会造成更多的ADMM迭代，但每一步时间更快，总时间减少
//    xx = method.gredientDescent(xx, df)

    //使用DFP方法
//            val method = new DFP(ifFixedStep = false,ifPositiveValue = true,precision = 1.0E-1)
//            xx = method.gredientDescent(xx, df)

//        val method = new Momentum(ifFixedStep = true,ifPositiveValue = false,precision = 1.0E-2)
//        xx = method.gredientDescent(xx, df)


        val dfp = new DiffFunction[DenseVector[Double]] {
      override def calculate(x: DenseVector[Double]): (Double, DenseVector[Double]) = {
        (df.computeObjective(x) ,df.computeGradient(x))
      }
    }
    val lbfgs = new LBFGS[DenseVector[Double]](maxIter=100.toInt, m=7,tolerance = 1.0E-1)
    // m is the memory. anywhere between 3 and 7 is fine. The larger m, the more memory is needed.
    xx = lbfgs.minimize(dfp,xx)

//    do {
//      step += 1
//      val method = new SteepestDescent()
//      xx = method.gredientDescent(xx, df,ifFixedStep = true)
//      u = u * 0.6
//      error = abs(- sum(u*log(xx)))
//      println(name+":"+xx)
//      println("SUMT step:"+step+",error:"+error+",obj:"+df.computeObjective(xx)+",u:"+u)
//    } while (error > 1.0E-2 && step < maxStep)
    //println(name+":"+xx)
    xx
  }

}
