package controllers

import edge.pay.ErgoPayResponse
import errors.ExceptionThrowable
import feed.Feed
import io.circe.Json
import io.circe.syntax.EncoderOps
import mint.{Client, NFTMinter}
import org.ergoplatform.appkit.{Address, ReducedTransaction}
import play.api.libs.circe.Circe
import play.api.Logger

import javax.inject._
import play.api.mvc._

class HomeController @Inject() (
  val client: Client,
  val NFTMinter: NFTMinter,
  val feed: Feed,
  val controllerComponents: ControllerComponents
) extends BaseController
    with Circe
    with ExceptionThrowable {
  client.setClient()
  private val logger: Logger = Logger(this.getClass)

  def index(): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] => Ok(views.html.index())
  }

  def getFeed: Action[Json] = Action(circe.json) {
    implicit request: Request[Json] =>
      try {
        val address: String = getRequestBodyAsString(request, "address")

        Ok(Json.fromString(address)).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def getAddressFeed(walletAddress: String): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      try {
        val tweetJson = feed.get(Address.create(walletAddress))

        Ok(Json.fromFields(List(
          ("tweets", Json.fromValues(tweetJson))
        ))).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def tweet: Action[Json] = Action(circe.json) {
    implicit request: Request[Json] =>
      try {
        val message: String = getRequestBodyAsString(request, "message")
        val walletAddress: String =
          getRequestBodyAsString(request, "walletAddress")

        val tweetTx: Seq[(Address, ReducedTransaction)] =
          NFTMinter.mintTweetTx(
            message = message,
            address = walletAddress
          )

        val ergoPayResponse: Seq[ErgoPayResponse] = tweetTx.map(tweet =>
          ErgoPayResponse.getResponse(
            recipient = tweet._1,
            reducedTx = tweet._2,
            message = s"Tweet: ${message}",
            replyTo = walletAddress
          )
        )

        Ok(ergoPayResponse.asJson).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def reply: Action[Json] = Action(circe.json) {
    implicit request: Request[Json] =>
      try {
        val message: String = getRequestBodyAsString(request, "message")
        val tweetId: String = getRequestBodyAsString(request, "tweetId")
        val walletAddress: String =
          getRequestBodyAsString(request, "walletAddress")

        val tweetTx: Seq[(Address, ReducedTransaction)] =
          NFTMinter.reply(
            tweetId = tweetId,
            message = message,
            address = walletAddress
          )

        val ergoPayResponse: Seq[ErgoPayResponse] = tweetTx.map(tweet =>
          ErgoPayResponse.getResponse(
            recipient = tweet._1,
            reducedTx = tweet._2,
            message = s"Tweet: ${message}",
            replyTo = walletAddress
          )
        )

        Ok(ergoPayResponse.asJson).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def retweet: Action[Json] = Action(circe.json) {
    implicit request: Request[Json] =>
      try {
        val message: String = getRequestBodyAsString(request, "message")
        val tweetId: String = getRequestBodyAsString(request, "tweetId")
        val walletAddress: String =
          getRequestBodyAsString(request, "walletAddress")

        val tweetTx: Seq[(Address, ReducedTransaction)] =
          NFTMinter.retweet(
            tweetId = tweetId,
            message = message,
            address = walletAddress
          )

        val ergoPayResponse: Seq[ErgoPayResponse] = tweetTx.map(tweet =>
          ErgoPayResponse.getResponse(
            recipient = tweet._1,
            reducedTx = tweet._2,
            message = s"Tweet: ${message}",
            replyTo = walletAddress
          )
        )

        Ok(ergoPayResponse.asJson).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def deleteTweet(tweetId: String): Action[Json] = Action(circe.json) {
    implicit request: Request[Json] =>
      try {
        val tweetId: String = getRequestBodyAsString(request, "tweetId")
        val walletAddress: String =
          getRequestBodyAsString(request, "walletAddress")

        val tweetTx: Seq[(Address, ReducedTransaction)] =
          NFTMinter.delete(
            tweetId = tweetId,
            address = walletAddress
          )

        val ergoPayResponse: Seq[ErgoPayResponse] = tweetTx.map(tweet =>
          ErgoPayResponse.getResponse(
            recipient = tweet._1,
            reducedTx = tweet._2,
            message = s"Tweet: burning ${tweetId}",
            replyTo = walletAddress
          )
        )

        Ok(ergoPayResponse.asJson).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def createProfile: Action[Json] = Action(circe.json) {
    implicit request: Request[Json] =>
      try {
        val address: String = getRequestBodyAsString(request, "address")
        val nftId: String = getRequestBodyAsString(request, "nftId")

        Ok(Json.fromString(address)).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def deleteProfile: Action[Json] = Action(circe.json) {
    implicit request: Request[Json] =>
      try {
        val address: String = getRequestBodyAsString(request, "address")
        val nftId: String = getRequestBodyAsString(request, "nftId")

        Ok(Json.fromString(address)).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def changeProfileNFT: Action[Json] = Action(circe.json) {
    implicit request: Request[Json] =>
      try {
        val address: String = getRequestBodyAsString(request, "address")
        val nftId: String = getRequestBodyAsString(request, "nftId")

        Ok(Json.fromString(address)).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def follow: Action[Json] = Action(circe.json) {
    implicit request: Request[Json] =>
      try {
        val addressToFollow: String =
          getRequestBodyAsString(request, "addressToFollow")
        val walletAddress: String =
          getRequestBodyAsString(request, "walletAddress")

        Ok(Json.fromString(walletAddress)).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def unfollow: Action[Json] = Action(circe.json) {
    implicit request: Request[Json] =>
      try {
        val addressToUnfollow: String =
          getRequestBodyAsString(request, "addressToUnfollow")
        val walletAddress: String =
          getRequestBodyAsString(request, "walletAddress")

        Ok(Json.fromString(walletAddress)).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }
}
