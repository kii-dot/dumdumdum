package controllers

import edge.pay.ErgoPayResponse
import errors.ExceptionThrowable
import feed.Feed
import io.circe.Json
import io.circe.syntax.EncoderOps
import mint.{Client, NFT, NFTMinter}
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

  def getProfile(walletAddress: String): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      try {
        val nft: NFT =
          profile.getProfileNFTDetails(address = Address.create(walletAddress))
        val followingRegister =
          profile.getFollowing(Address.create(walletAddress))

        Ok(
          Json.fromFields(
            List(
              ("profileNFT", nft.toJson),
              ("following", followingRegister.toJson)
            )
          )
        ).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def getFeed(walletAddress: String): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      try {
        val followingRegister =
          profile.getFollowing(Address.create(walletAddress))
        val tweets = followingRegister.collAddress
          .flatMap(address => feed.get(address))
          .sortBy(tweet => tweet.creationHeight)(Ordering.Long.reverse)

        Ok(
          Json.fromFields(
            List(
              ("tweets", Json.fromValues(tweets.map(tweet => tweet.toJson)))
            )
          )
        ).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def getAddressFeed(walletAddress: String): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      try {
        val tweets = feed.get(Address.create(walletAddress))

        Ok(
          Json.fromFields(
            List(
              ("tweets", Json.fromValues(tweets.map(tweet => tweet.toJson)))
            )
          )
        ).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def getAddressFollowing(walletAddress: String): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] =>
      try {
        val followingRegister =
          profile.getFollowing(Address.create(walletAddress))

        Ok(
          followingRegister.toJson
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

        val ergoPayResponse: Seq[ErgoPayResponse] =
          tweetTx.zipWithIndex.map(tweet => getErgoPayResponse(tweet, message))

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

        val ergoPayResponse: Seq[ErgoPayResponse] =
          tweetTx.zipWithIndex.map(tweet => getErgoPayResponse(tweet, message))

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

        val ergoPayResponse: Seq[ErgoPayResponse] =
          tweetTx.zipWithIndex.map(tweet => getErgoPayResponse(tweet, message))

        Ok(ergoPayResponse.asJson).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def deleteTweet(): Action[Json] = Action(circe.json) {
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

        val ergoPayResponse: Seq[ErgoPayResponse] =
          tweetTx.zipWithIndex.map(tweet =>
            getErgoPayResponse(tweet, "Deleting Tweet")
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

        val ergoPayResponse: Seq[ErgoPayResponse] =
          createProfileTx.zipWithIndex.map(tx =>
            getErgoPayResponse(tx, "DumDumDum: Creating Profile")
          )

        Ok(ergoPayResponse.asJson).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }

  def deleteProfile(): Action[Json] = Action(circe.json) {
    implicit request: Request[Json] =>
      try {
        val address: String = getRequestBodyAsString(request, "address")

        val deleteProfileTx: Seq[(Address, ReducedTransaction)] =
          profile.delete(
            address = Address.create(address)
          )

        val ergoPayResponse: Seq[ErgoPayResponse] =
          deleteProfileTx.zipWithIndex.map(tx =>
            getErgoPayResponse(tx, "DumDumDum: Delete Profile")
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

        val ergoPayResponse: Seq[ErgoPayResponse] =
          changeProfileNFTTx.zipWithIndex.map(tx =>
            getErgoPayResponse(tx, "DumDumDum: Changing Profile NFT")
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

        val ergoPayResponse: Seq[ErgoPayResponse] =
          followTx.zipWithIndex.map(tx =>
            getErgoPayResponse(tx, s"DumDumDum: Following ${addressToFollow}")
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

        val ergoPayResponse: Seq[ErgoPayResponse] =
          unfollowTx.zipWithIndex.map(tx =>
            getErgoPayResponse(
              tx,
              s"DumDumDum: Unfollowing ${addressToUnfollow}"
            )
          )

        Ok(ergoPayResponse.asJson).as("application/json")
      } catch {
        case e: Throwable => exception(e, logger)
      }
  }
}
