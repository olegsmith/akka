/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.remote

import akka.AkkaException
import akka.actor._
import akka.event.{ Logging, LoggingAdapter }
import scala.collection.immutable
import scala.concurrent.Future
import scala.util.control.NoStackTrace

/**
 * RemoteTransportException represents a general failure within a RemoteTransport,
 * such as inability to start, wrong configuration etc.
 */
@SerialVersionUID(1L)
class RemoteTransportException(message: String, cause: Throwable) extends AkkaException(message, cause)

/**
 * [[RemoteTransportException]] without stack trace.
 */
@SerialVersionUID(1L)
class RemoteTransportExceptionNoStackTrace(message: String, cause: Throwable)
  extends RemoteTransportException(message, cause) with NoStackTrace

/**
 * INTERNAL API
 *
 * The remote transport is responsible for sending and receiving messages.
 * Each transport has an address, which it should provide in
 * Serialization.currentTransportInformation (thread-local) while serializing
 * actor references (which might also be part of messages). This address must
 * be available (i.e. fully initialized) by the time the first message is
 * received or when the start() method returns, whatever happens first.
 */
private[akka] abstract class RemoteTransport(val system: ExtendedActorSystem, val provider: RemoteActorRefProvider) {
  /**
   * Shuts down the remoting
   */
  def shutdown(): Future[Unit]

  /**
   * Address to be used in RootActorPath of refs generated for this transport.
   */
  def addresses: immutable.Set[Address]

  /**
   * The default transport address of the actorsystem
   * @return The listen address of the default transport
   */
  def defaultAddress: Address

  /**
   * Resolves the correct local address to be used for contacting the given remote address
   * @param remote the remote address
   * @return the local address to be used for the given remote address
   */
  def localAddressForRemote(remote: Address): Address

  /**
   * Start up the transport, i.e. enable incoming connections.
   */
  def start(): Unit

  /**
   * Sends the given message to the recipient supplying the sender() if any
   */
  def send(message: Any, senderOption: Option[ActorRef], recipient: RemoteActorRef): Unit

  /**
   * Sends a management command to the underlying transport stack. The call returns with a Future that indicates
   * if the command was handled successfully or dropped.
   * @param cmd Command message to send to the transports.
   * @return A Future that indicates when the message was successfully handled or dropped.
   */
  def managementCommand(cmd: Any): Future[Boolean] = { Future.successful(false) }

  /**
   * A Logger that can be used to log issues that may occur
   */
  def log: LoggingAdapter

  /**
   * Marks a remote system as out of sync and prevents reconnects until the quarantine timeout elapses.
   * @param address Address of the remote system to be quarantined
   * @param uid UID of the remote system, if the uid is not defined it will not be a strong quarantine but
   *   the current endpoint writer will be stopped (dropping system messages) and the address will be gated
   */
  def quarantine(address: Address, uid: Option[Int]): Unit

  /**
   * When this method returns true, some functionality will be turned off for security purposes.
   */
  protected def useUntrustedMode: Boolean

  /**
   * When this method returns true, RemoteLifeCycleEvents will be logged as well as be put onto the eventStream.
   */
  protected def logRemoteLifeCycleEvents: Boolean

}
