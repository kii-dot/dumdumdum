import edge.pay.ErgoPayResponse
import io.circe.Json
import org.ergoplatform.appkit.{Address, ReducedTransaction}
import play.api.mvc.{AnyContent, Request}

package object controllers {

  def getErgoPayResponse(
    tweet: ((Address, ReducedTransaction), Int),
    message: String
  ): ErgoPayResponse =
    ErgoPayResponse.getResponse(
      recipient = tweet._1._1,
      reducedTx = tweet._1._2,
      message = s"${tweet._2} Tweet: $message",
      replyTo = tweet._1._1.toString
    )

  def getRequestHeaderAsString(
    request: Request[AnyContent],
    key: String,
    errorMessage: String = ""
  ): String = {
    val processedErrorMessage = processErrorMessage(key, errorMessage)
    request.headers
      .get(key)
      .getOrElse(throw new Throwable(processedErrorMessage))
  }

  def getRequestHeaderAsInt(
    request: Request[AnyContent],
    key: String,
    errorMessage: String = ""
  ): Int = {
    val processedErrorMessage = processErrorMessage(key, errorMessage)
    request.headers
      .get(key)
      .getOrElse(throw new Throwable(processedErrorMessage))
      .toInt
  }

  def getRequestHeaderAsLong(
    request: Request[AnyContent],
    key: String,
    errorMessage: String = ""
  ): Long = {
    val processedErrorMessage = processErrorMessage(key, errorMessage)
    request.headers
      .get(key)
      .getOrElse(throw new Throwable(processedErrorMessage))
      .toLong
  }

  def getRequestHeaderAsFloat(
    request: Request[AnyContent],
    key: String,
    errorMessage: String = ""
  ): Float = {
    val processedErrorMessage = processErrorMessage(key, errorMessage)
    request.headers
      .get(key)
      .getOrElse(throw new Throwable(processedErrorMessage))
      .toFloat
  }

  def getRequestHeaderAsDouble(
    request: Request[AnyContent],
    key: String,
    errorMessage: String = ""
  ): Double = {
    val processedErrorMessage = processErrorMessage(key, errorMessage)
    request.headers
      .get(key)
      .getOrElse(throw new Throwable(processedErrorMessage))
      .toDouble
  }

  def getRequestBodyAsString(
    request: Request[Json],
    key: String,
    errorMessage: String = ""
  ): String = {
    val processedErrorMessage = processErrorMessage(key, errorMessage)
    request.body.hcursor
      .downField(key)
      .as[String]
      .getOrElse(throw new Throwable(processedErrorMessage))
  }

  def getRequestBodyAsInt(
    request: Request[Json],
    key: String,
    errorMessage: String = ""
  ): Int = {
    val processedErrorMessage = processErrorMessage(key, errorMessage)
    request.body.hcursor
      .downField(key)
      .as[Int]
      .getOrElse(throw new Throwable(processedErrorMessage))
  }

  def getRequestBodyAsLong(
    request: Request[Json],
    key: String,
    errorMessage: String = ""
  ): Long = {
    val processedErrorMessage = processErrorMessage(key, errorMessage)
    request.body.hcursor
      .downField(key)
      .as[Long]
      .getOrElse(throw new Throwable(processedErrorMessage))
  }

  def getRequestBodyAsFloat(
    request: Request[Json],
    key: String,
    errorMessage: String = ""
  ): Float = {
    val processedErrorMessage = processErrorMessage(key, errorMessage)
    request.body.hcursor
      .downField(key)
      .as[Float]
      .getOrElse(throw new Throwable(processedErrorMessage))
  }

  def getRequestBodyAsDouble(
    request: Request[Json],
    key: String,
    errorMessage: String = ""
  ): Double = {
    val processedErrorMessage = processErrorMessage(key, errorMessage)
    request.body.hcursor
      .downField(key)
      .as[Double]
      .getOrElse(throw new Throwable(processedErrorMessage))
  }

  def processErrorMessage(key: String, errorMessage: String): String =
    if (errorMessage.isEmpty())
      s"$key field must exist"
    else
      errorMessage.toString()
}
