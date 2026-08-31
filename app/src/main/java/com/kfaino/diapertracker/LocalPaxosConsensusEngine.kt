package com.kfaino.diapertracker

import java.util.concurrent.atomic.AtomicLong

/**
 * 🏛️ 家庭局域网多机去中心化 Paxos/Raft 共识对撞引擎 (Local Paxos Consensus Engine)
 */
object LocalPaxosConsensusEngine {

    data class PaxosProposal(
        val proposalNumber: Long,
        val proposerDeviceId: String,
        val statePayloadJson: String
    )

    data class PaxosPromise(
        val accepted: Boolean,
        val highestPromisedNumber: Long,
        val lastAcceptedValue: String?
    )

    private val proposalCounter = AtomicLong(1L)
    private var promisedNumber = 0L
    private var acceptedNumber = 0L
    private var acceptedValue: String? = null

    fun prepare(proposerId: String, payload: String): PaxosProposal {
        val num = proposalCounter.incrementAndGet()
        return PaxosProposal(num, proposerId, payload)
    }

    fun onReceivePrepare(proposal: PaxosProposal): PaxosPromise {
        return if (proposal.proposalNumber > promisedNumber) {
            promisedNumber = proposal.proposalNumber
            PaxosPromise(true, promisedNumber, acceptedValue)
        } else {
            PaxosPromise(false, promisedNumber, null)
        }
    }
}