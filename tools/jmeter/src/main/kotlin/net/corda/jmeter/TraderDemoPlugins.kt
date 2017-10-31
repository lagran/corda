package net.corda.jmeter

import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.getOrThrow
import net.corda.finance.DOLLARS
import net.corda.finance.flows.CashIssueFlow
import net.corda.jmeter.CordaRPCSampler.FlowInvoke
import net.corda.testing.DUMMY_BANK_A
import net.corda.testing.DUMMY_BANK_B
import net.corda.testing.contracts.calculateRandomlySizedAmounts
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext
import java.util.*


abstract class AsbtractTraderDemoPlugin : CordaRPCSampler.Plugin {

    lateinit var buyer: Party
    lateinit var seller: Party
    lateinit var notary: Party

    protected fun getIdentities(rpc: CordaRPCOps) {
        buyer = rpc.wellKnownPartyFromX500Name(DUMMY_BANK_A.name) ?: throw IllegalStateException("Don't know ${DUMMY_BANK_A.name}")
        seller = rpc.wellKnownPartyFromX500Name(DUMMY_BANK_B.name) ?: throw IllegalStateException("Don't know ${DUMMY_BANK_B.name}")
        notary = rpc.notaryIdentities().first()
    }

}

class CashIssuerPlugin : AsbtractTraderDemoPlugin() {
    override fun setupTest(rpcProxy: CordaRPCOps, testContext: JavaSamplerContext) {
        getIdentities(rpcProxy)
    }

    override fun teardownTest(rpcProxy: CordaRPCOps, testContext: JavaSamplerContext) {
    }

    override fun createFlowInvoke(rpcProxy: CordaRPCOps, testContext: JavaSamplerContext): FlowInvoke<CashIssueFlow> {
        val amount = 1_100_000_000_000.DOLLARS
        //val amounts = calculateRandomlySizedAmounts(amount, 3, 10, Random())
        //rpc.startFlow(net.corda.finance.flows::CashIssueFlow, amount, OpaqueBytes.of(1), notary).returnValue.getOrThrow()
        return FlowInvoke<CashIssueFlow>(CashIssueFlow::class.java, arrayOf(amount, OpaqueBytes.of(1), notary))
    }

}