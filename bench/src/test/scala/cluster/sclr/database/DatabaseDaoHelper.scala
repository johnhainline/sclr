package cluster.sclr.database

import scala.util.Random

class DatabaseDaoHelper(random: Random) {

  def fakeDataset(size: Int, xLength: Int, yLength: Int): Dataset = {
    Dataset(fakeData(size, xLength, yLength), xLength, yLength)
  }

  def fakeData(size: Int, xLength: Int, yLength: Int): Array[XYZ] = {
    val data = new Array[XYZ](size)
    for (i <- 0 until size) {
      val x = new Array[Boolean](xLength)
      val y = new Array[Double](yLength)
      for (ix <- x.indices) {
        x(ix) = random.nextBoolean()
      }
      for (iy <- y.indices) {
        y(iy) = random.nextDouble()
      }
      data(i) = XYZ(i, x, y, random.nextDouble())
    }
    data
  }

}
