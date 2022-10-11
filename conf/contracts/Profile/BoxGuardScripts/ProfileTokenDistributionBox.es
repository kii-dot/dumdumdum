{
    // ===== Contract Info ===== //
    // Name             : Profile Token Distribution Box
    // Description      : A box that contains the Profile Token to be
    //                  distributed to create profile boxes. The tokens
    //                  in this box should only allow distribution of one
    //                  token per user (if possible), and only allow one
    //                  token per box per address.
    // Type             : Guard Script
    // Author           : Kii
    // Last Modified    : Oct 10th 2022
    // Version          : v 1.0
    // Status           : Implementing

    // ===== Contract Hard-Coded Constants ===== //
    // val _OwnerPK:                        Coll[Byte]
    // val _DumDumDumNFT:                   Coll[Byte]
    // val _DumDumDumProfileToken:          Coll[Byte]

    // ===== Box Details ===== //
    // No registers needed

    // ===== Contract Conditions ===== //
    // 1. Creation of Profile Box
    // 2. Nada

    // ====== Creation ====== //
    // Inputs: ProfileDistributionBox(0), TxFee(1)
    // Outputs: ProfileDistributionBox(0), ProfileBox(1), MiningBox(2)
    val _creationScenario = OUTPUTS.size == 3

    // ====== Mutation ====== //
    // Inputs: ProfileDistributionBox(0)
    // Outputs: ProfileDistributionBox(0), MiningBox(2)
    val _mutationScenario = OUTPUTS.size == 2

    // ====== Consumption ====== //
    // Inputs: ProfileDistributionBox(0)
    // Outputs: MiningBox(2)
    val _consumptionScenario = OUTPUTS.size == 1

    if (_mutationScenario || _consumptionScenario) {
        _OwnerPK
    } else if (_creationScenario) {
        // ====== Creation ====== //
        // Inputs: ProfileDistributionBox(0), TxFee(1)
        // Outputs: ProfileDistributionBox(0), ProfileBox(1), MiningBox(2)
        //
        // Conditions:
        // 1. X There can only be one distribution of the token
        // 2. R4 have to have an NFTId (Coll[Byte])
        // 3. R5 have to have an AVL Tree instantiated
        // 4. X Total token of self in output is (total - 1)
        // 5. X Address is the same
        //
        // Note: We don't need to worry about Kra's bot because
        //      this tx requires a direct box from the creator.
        //      therefore most if not all txs can't be manipulated.

        val _inProfileDistributionBox = INPUTS(0)

        val _profileBox = OUTPUTS(1)
        val _outProfileDistributionBox = OUTPUTS(0)

        // Profile Distribution Box check
        val isProfileDistributionBoxAddressSame: Boolean =
            _inProfileDistributionBox.propositionBytes == _outProfileDistributionBox.propositionBytes

        val isProfileDistributionBoxValueSame: Boolean =
            _inProfileDistributionBox.value == _outProfileDistributionBox.value

        val isOnly1TokenDistributed: Boolean =
            _outProfileDistributionBox.tokens(1)._2 == _inProfileDistributionBox.tokens(1)._2 - 1

        val isTokenSame: Boolean = allOf(Coll(
            _inProfileDistributionBox.tokens(0)._1 == _outProfileDistributionBox.tokens(0)._1,
            _inProfileDistributionBox.tokens(0)._2 == _outProfileDistributionBox.tokens(0)._2,
            _inProfileDistributionBox.tokens(1)._1 == _outProfileDistributionBox.tokens(1)._1,
        ))

        val profileDistributionBoxCheck: Boolean = allOf(Coll(
            isProfileDistributionBoxAddressSame,
            isProfileDistributionBoxValueSame,
            isTokenSame
        ))

        // Profile Box Check
        val isProfileBoxTokenReceived: Boolean = allOf(Coll(
            _inProfileDistributionBox.tokens(1)._1 == _profileBox.tokens(0)._1,
            _profileBox.tokens(0)._2 == 1
        ))

        val isProfileBoxNFTAdded: Boolean = allOf(Coll(
            _profileBox.R4[Coll[Byte]].isDefined
        ))

        val profileBoxCheck: Boolean = allOf(Coll(
            isProfileBoxTokenReceived,
            isProfileBoxNFTAdded
        ))


        sigmaProp(allOf(Coll(
            profileDistributionBoxCheck,
            isOnly1TokenDistributed,
            profileBoxCheck
        )))

    } else {
        sigmaProp(false)
    }
}