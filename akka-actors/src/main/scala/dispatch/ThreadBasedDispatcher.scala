/**
 * Copyright (C) 2009 Scalable Solutions.
 */

package se.scalablesolutions.akka.dispatch

import java.util.concurrent.LinkedBlockingQueue
import java.util.Queue

import se.scalablesolutions.akka.actor.{Actor, ActorMessageInvoker}

/**
 * Dedicates a unique thread for each actor passed in as reference. Served through its messageQueue.
 * 
 * @author <a href="http://jonasboner.com">Jonas Bon&#233;r</a>
 */
class ThreadBasedDispatcher private[akka] (val name: String, val messageHandler: MessageInvoker) 
  extends MessageDispatcher {
  
  def this(actor: Actor) = this(actor.getClass.getName, new ActorMessageInvoker(actor))

  private val queue = new BlockingMessageQueue(name)
  private var selectorThread: Thread = _
  @volatile private var active: Boolean = false

  def dispatch(invocation: MessageInvocation) = queue.append(invocation) 

  def start = if (!active) {
    active = true
    selectorThread = new Thread {
      override def run = {
        while (active) {
          try {
            messageHandler.invoke(queue.take)
          } catch { case e: InterruptedException => active = false }
        }
      }
    }
    selectorThread.start
  }
                       
  def shutdown = if (active) {
    active = false
    selectorThread.interrupt
  }
}

class BlockingMessageQueue(name: String) extends MessageQueue {
  // FIXME: configure the LinkedBlockingQueue in BlockingMessageQueue, use a Builder like in the ReactorBasedThreadPoolEventDrivenDispatcher
  private val queue = new LinkedBlockingQueue[MessageInvocation]
  def append(invocation: MessageInvocation) = queue.put(invocation)
  def prepend(invocation: MessageInvocation) = queue.add(invocation) // FIXME is add prepend???
  def take: MessageInvocation = queue.take
  def read(destination: Queue[MessageInvocation]) = throw new UnsupportedOperationException
  def interrupt = throw new UnsupportedOperationException
}
