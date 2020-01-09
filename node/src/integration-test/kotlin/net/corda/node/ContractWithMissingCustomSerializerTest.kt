package net.corda.node

import net.corda.client.rpc.CordaRPCClient
import net.corda.contracts.serialization.missing.CustomData
import net.corda.core.CordaRuntimeException
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.internal.hash
import net.corda.core.internal.inputStream
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import net.corda.flows.serialization.missing.MissingSerializerBuilderFlow
import net.corda.flows.serialization.missing.MissingSerializerFlow
import net.corda.node.services.Permissions
import net.corda.testing.core.ALICE_NAME
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.driver.internal.incrementalPortAllocation
import net.corda.testing.node.NotarySpec
import net.corda.testing.node.TestCordapp
import net.corda.testing.node.User
import net.corda.testing.node.internal.cordappWithFixups
import net.corda.testing.node.internal.cordappWithPackages
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.jupiter.api.assertThrows

@Suppress("FunctionName")
class ContractWithMissingCustomSerializerTest {
    companion object {
        const val BOBBINS = 5000L

        val user = User("u", "p", setOf(Permissions.all()))
        val flowCorDapp = cordappWithPackages("net.corda.flows.serialization.missing").signed()
        val contractCorDapp = cordappWithPackages("net.corda.contracts.serialization.missing").signed()

        fun driverParameters(cordapps: List<TestCordapp>): DriverParameters {
            return DriverParameters(
                portAllocation = incrementalPortAllocation(),
                startNodesInProcess = false,
                notarySpecs = listOf(NotarySpec(DUMMY_NOTARY_NAME, validating = true)),
                cordappsForAllNodes = cordapps
            )
        }
    }

    /*
     * Test that we can still verify a transaction that is missing a custom serializer.
     */
    @Test
    fun `flow with missing custom serializer by rpc`() {
        val contractId = contractCorDapp.jarFile.hash
        val flowId = flowCorDapp.jarFile.hash
        val fixupCorDapp = cordappWithFixups(listOf(setOf(contractId) to setOf(contractId, flowId))).signed()

        driver(driverParameters(listOf(flowCorDapp, contractCorDapp, fixupCorDapp))) {
            val alice = startNode(providedName = ALICE_NAME, rpcUsers = listOf(user)).getOrThrow()
            val ex = assertThrows<TransactionVerificationException> {
                CordaRPCClient(hostAndPort = alice.rpcAddress)
                    .start(user.username, user.password)
                    .use { client ->
                        with(client.proxy) {
                            uploadAttachment(flowCorDapp.jarFile.inputStream())
                            startFlow(::MissingSerializerFlow, BOBBINS).returnValue.getOrThrow()
                        }
                    }
            }
            assertThat(ex).hasMessageContaining("Data $BOBBINS bobbins exceeds maximum value!")
        }
    }

    /*
     * Test that TransactionBuilder prevents us from creating a
     * transaction that has a custom serializer missing.
     */
    @Test
    fun `transaction builder flow with missing custom serializer by rpc`() {
        driver(driverParameters(listOf(flowCorDapp, contractCorDapp))) {
            val alice = startNode(providedName = ALICE_NAME, rpcUsers = listOf(user)).getOrThrow()
            val ex = assertThrows<CordaRuntimeException> {
                CordaRPCClient(hostAndPort = alice.rpcAddress)
                    .start(user.username, user.password)
                    .proxy
                    .startFlow(::MissingSerializerBuilderFlow, BOBBINS)
                    .returnValue
                    .getOrThrow()
            }
            assertThat(ex)
                .hasMessageContaining("TransactionDeserialisationException:")
                .hasMessageFindingMatch(CustomData::class.java.name)
        }
    }
}