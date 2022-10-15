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
import profile.Profile

class HomeController @Inject() (
  val client: Client,
  val NFTMinter: NFTMinter,
  val feed: Feed,
  val profile: Profile,
  val controllerComponents: ControllerComponents
) extends BaseController
    with Circe
    with ExceptionThrowable {
  client.setClient()
  private val logger: Logger = Logger(this.getClass)

  def index(): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] => Ok(views.html.index())
  }

  def ping: Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      Ok(Json.fromString("pong")).as("application/json")
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

        Ok(
          Json.fromFields(
            List(
              ("tweets", Json.fromValues(tweetJson))
            )
          )
        ).as("application/json")
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
        val createProfileTx: Seq[(Address, ReducedTransaction)] =
          profile.create(
            address = Address.create(address),
            nftId = nftId
          )

        val ergoPayResponse: Seq[ErgoPayResponse] = createProfileTx.map(tx =>
          ErgoPayResponse.getResponse(
            recipient = tx._1,
            reducedTx = tx._2,
            message = "DumDumDum: Create Profile",
            replyTo = address
          )
        )

        Ok(ergoPayResponse.asJson).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def deleteProfile: Action[Json] = Action(circe.json) {
    implicit request: Request[Json] =>
      try {
        val address: String = getRequestBodyAsString(request, "address")

        val deleteProfileTx: Seq[(Address, ReducedTransaction)] =
          profile.delete(
            address = Address.create(address)
          )

        val ergoPayResponse: Seq[ErgoPayResponse] = deleteProfileTx.map(tx =>
          ErgoPayResponse.getResponse(
            recipient = tx._1,
            reducedTx = tx._2,
            message = "DumDumDum: Delete Profile",
            replyTo = address
          )
        )

        Ok(ergoPayResponse.asJson).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def changeProfileNFT: Action[Json] = Action(circe.json) {
    implicit request: Request[Json] =>
      try {
        val address: String = getRequestBodyAsString(request, "address")
        val nftId: String = getRequestBodyAsString(request, "nftId")

        val changeProfileNFTTx: Seq[(Address, ReducedTransaction)] =
          profile.changeProfileNFT(
            address = Address.create(address),
            nftId = nftId
          )

        val ergoPayResponse: Seq[ErgoPayResponse] = changeProfileNFTTx.map(tx =>
          ErgoPayResponse.getResponse(
            recipient = tx._1,
            reducedTx = tx._2,
            message = "DumDumDum: Delete Profile",
            replyTo = address
          )
        )

        Ok(ergoPayResponse.asJson).as("application/json")
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

        val followTx: Seq[(Address, ReducedTransaction)] =
          profile.follow(
            walletAddress = Address.create(walletAddress),
            addressToFollow = Address.create(addressToFollow)
          )

        val ergoPayResponse: Seq[ErgoPayResponse] = followTx.map(tx =>
          ErgoPayResponse.getResponse(
            recipient = tx._1,
            reducedTx = tx._2,
            message = "DumDumDum: Delete Profile",
            replyTo = walletAddress
          )
        )

        Ok(ergoPayResponse.asJson).as("application/json")
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

        val unfollowTx: Seq[(Address, ReducedTransaction)] =
          profile.unfollow(
            walletAddress = Address.create(walletAddress),
            addressToUnfollow = Address.create(addressToUnfollow)
          )

        val ergoPayResponse: Seq[ErgoPayResponse] = unfollowTx.map(tx =>
          ErgoPayResponse.getResponse(
            recipient = tx._1,
            reducedTx = tx._2,
            message = "DumDumDum: Delete Profile",
            replyTo = walletAddress
          )
        )

        Ok(ergoPayResponse.asJson).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }
}
