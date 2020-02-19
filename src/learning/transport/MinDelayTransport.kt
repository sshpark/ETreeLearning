package learning.transport

import peersim.core.Node
import peersim.transport.Transport

/**
 * @author sshpark
 * @date 19/2/2020
 *
 */

class MinDelayTransport: Transport{
    override fun getLatency(src: Node?, dest: Node?): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clone(): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun send(src: Node?, dest: Node?, msg: Any?, pid: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}