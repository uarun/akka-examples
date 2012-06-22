package com.uarun.examples.akka

import akka.actor._

import akka.dispatch.Await
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration._

sealed trait GreeterMessage
case object Initialize extends GreeterMessage
case object ReInitialize extends GreeterMessage
case class Greet(who: String) extends GreeterMessage
case object Pause extends GreeterMessage

class Greeter extends Actor {
  import context._
  val log = Logging(context.system, this)

  def unintialized: Receive = {
    case Initialize => 
      log.info("Actor initializing ...")
      Thread.sleep(2000)
      become(initialized)
      log.info("Actor initialized")
  }

  def initialized : Receive = {
    case Greet(who) => 
      Thread.sleep(1000)
      sender ! "Howdy, " + who

    case ReInitialize => 
      become(unintialized)
      self ! Initialize

    case Pause => 
      log.info("Actor paused")
      become(unintialized)

    case _ => log.info("Received invalid message")
  }

  def receive = unintialized
}

object BecomingActor {
  val system = ActorSystem()

  def main(args: Array[String]) {
    println("Actor System: " + system)
    val greeter = system.actorOf(Props[Greeter], name = "greeter")
    println("Greeter: " + greeter)

    greeter ! Greet("World")
    greeter ! 10

    greeter ! Initialize

    implicit val timeout = Timeout(5 seconds)
    val future = greeter ? Greet("Mars")  // enabled by 'ask' import
    val result = Await.result(future, timeout.duration).asInstanceOf[String]

    println("Result from future: " + result)

    greeter ! Pause
    greeter ? Greet("Mars") andThen printReply andThen printDone

    greeter ! Initialize
    Thread.sleep(3000)
    greeter ? Greet("Pluto") onComplete {
      case Right(result) => println("Actor replied with: " + result)
      case Left(failure) => println("Actor failed to reply: " + failure)
    }

    def printDone : PartialFunction[Either[Throwable, Any], Any]  = { case _ => println("All Done") }
    def printReply : PartialFunction[Either[Throwable, Any], Any] = { case Right(v) => println("Actor replied with: " + v) }

    Thread.sleep(10000)
    system.shutdown()
  }
}
