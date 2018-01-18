package weka

import org.scalatest.{FlatSpec, Matchers}

class RegressionSpec extends FlatSpec with Matchers {
  val file = "house.csv"
  val testObject = new Regression(file)

  "Regression" should "provide correct coefficients" in {
    val expectedCoefficients = Vector(195.2035045068723, 38.969439487067845, 0.0, 76218.46422989709, 73947.21179083386, 0.0, 2681.13595613739)

    testObject.coefficients should be (expectedCoefficients)
  }

  it should "give correct prediction" in {
    testObject.predict(testObject.instances.lastInstance()) should be (458013.1670394577)
  }

  it should "run linear regression on a subset correctly" in {
    val dimensions = Vector(0, 1, 4, 5)
    val rows = Vector(0, 3, 4, 5, 6)
    val expectedCoefficients = Vector(165.27691119958632, 66.68282889533515, 53242.54758808564, 0.0, -30775.194398564872)

    testObject.linearRegressionSubAttributes(dimensions, rows) should be (expectedCoefficients)
  }
}
