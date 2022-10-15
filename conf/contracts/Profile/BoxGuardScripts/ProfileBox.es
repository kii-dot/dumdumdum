{
    // ===== Contract Info ===== //
    // Name             : Profile box
    // Description      : A box that consists of data for a users Ergo-Twitter account.
    //                  Within each register, it consists of the users profile details.
    //                  Mainly, following, and NFT profile for V1
    //                  The main goal of this contract is to keep a contract separate
    //                  from the wallet, so that we don't accidentally spend it.
    //                  Therefore the implementation purely allows spending of the
    //                  owner, while introducing some checks that allows the contract
    //                  to live in a different address as compared to the user's wallet
    // Type             : Guard Script
    // Author           : Kii
    // Last Modified    : Oct 10th 2022
    // Version          : v 1.0
    // Status           : Implementing

    // ===== Contract Hard-Coded Constants ===== //
    // val _OwnerPK:                        Coll[Byte]
    // val _DumDumDumProfileToken:          Coll[Byte]

    // ===== Box Details ===== //
    // R4: Profile Picture (NFT Token Id) => Coll[Byte]
    // R5: Following (WalletAddress) => Coll[Coll[Byte]]

    // ===== Contract Conditions ===== //
    // 1. Adding/Removing Following
    // 2. Change Profile Picture

    val _minFee: Long       = 1000 * 1000
    val inProfileBox: Box   = INPUTS(0)
    val outProfileBox: Box  = OUTPUTS(0)

    // ====== Mutation ====== //
    // Inputs: ProfileBox(0), UserTxBox(1) -> Contains NFT || R4 follower
    // Outputs: ProfileBox(0), MiningBox(1)
    val isProfileBoxOutput: Boolean = allOf(Coll(
        inProfileBox.tokens(0)._1 == outProfileBox.tokens(0)._1,
        inProfileBox.tokens(0)._2 == outProfileBox.tokens(0)._2,
        inProfileBox.tokens(0)._1 == _DumDumDumProfileToken,
    ))

    val isMutation_Scenario: Boolean = isProfileBoxOutput

    if (isMutation_Scenario) {
        val isProfileBoxValueSame: Boolean =
            inProfileBox.value == outProfileBox.value

        val isProfileBoxAddressSame: Boolean =
            inProfileBox.propositionBytes == outProfileBox.propositionBytes

        val isProfileBoxReplicated: Boolean = allOf(Coll(
            isProfileBoxOutput,
            inProfileBox.R4[Coll[Byte]] == outProfileBox.R4[Coll[Byte]],
            inProfileBox.tokens.size <= 2,
            outProfileBox.tokens.size <= 2,
            isProfileBoxAddressSame,
            isProfileBoxValueSame
        ))

        // NFT Change
        val userTxBox: Box = INPUTS(1)
        val userOutBox: Box = OUTPUTS(1)
        val isNFTCheck: Boolean = allOf(Coll(
            outProfileBox.tokens.size <= 2,
            outProfileBox.tokens.size > 0,
            if (userTxBox.tokens.size > 0) {
                allOf(Coll(
                    outProfileBox.tokens(1)._1 == userTxBox.tokens(0)._1,
                    outProfileBox.tokens(1)._2 == userTxBox.tokens(0)._2,
                    userOutBox.tokens(0)._1 == inProfileBox.tokens(1)._1,
                    userOutBox.tokens(0)._2 == inProfileBox.tokens(1)._2,
                    userOutBox.propositionBytes == inProfileBox.R4[Coll[Byte]].get
                ))
            } else {
                allOf(Coll(
                    outProfileBox.tokens(1)._1 == inProfileBox.tokens(1)._1,
                    outProfileBox.tokens(1)._2 == inProfileBox.tokens(1)._2
                ))
            },
        ))

        // Follower check
        // If already exists, remove
        // if not, add
        // We need this hack else it will always fail if undefined
        val isUserBoxFollowerIsUser: Boolean =
            userTxBox.R4[Coll[Byte]].isDefined &&
            userTxBox.R4[Coll[Byte]].get == inProfileBox.R4[Coll[Byte]].get

        val isFollowerCheck: Boolean = allOf(Coll(
            if (isUserBoxFollowerIsUser) {
                allOf(Coll(
                    inProfileBox.R5[Coll[Coll[Byte]]] == outProfileBox.R5[Coll[Coll[Byte]]],
                ))
            } else {
                // Check if the address already exists
                val isAddressExistsInInput: Boolean =
                    inProfileBox.R5[Coll[Coll[Byte]]].get.exists{
                        (address: Coll[Byte]) => address == userTxBox.R4[Coll[Byte]].get
                    }

                val isAddressExistsInOutput: Boolean =
                    outProfileBox.R5[Coll[Coll[Byte]]].get.exists{
                        (address: Coll[Byte]) => address == userTxBox.R4[Coll[Byte]].get
                    }

                if (!isAddressExistsInInput)
                {
                    allOf(Coll(
                        // Add the address, Check if outBox has the address
                        isAddressExistsInOutput
                    ))
                } else {
                    allOf(Coll(
                        // remove the address, Check if outBox has the address
                        !isAddressExistsInOutput
                    ))
                }
            }
        ))

        sigmaProp(allOf(
            Coll(
                _OwnerPK,
                isNFTCheck,
                isFollowerCheck,
                isProfileBoxReplicated,
            )
        ))
    } else {
        // ======== Delete Profile ========
        // Conditions:
        // 1. dumDumDum tokens does not exist
        // 2. NFT goes back to owner
        // 3. ProfileBox does not exist
        val ownerBox: Box = OUTPUTS(0)
        val profileBox: Box = INPUTS(0)

        val isNFTReturned: Boolean = allOf(Coll(
            ownerBox.propositionBytes == profileBox.R4[Coll[Byte]].get,
            if (profileBox.tokens.size == 2) {
                allOf(Coll(
                    ownerBox.tokens.size == 1,
                    ownerBox.tokens(0)._1 == profileBox.tokens(1)._1,
                    ownerBox.tokens(0)._2 == profileBox.tokens(1)._2,
                ))
            } else {
                ownerBox.tokens.size == 0
            }
        ))

        // Inputs: ProfileBox(0), TxFee(1) -> To get the nft back
        // Outputs: OwnerBox(0) -> With NFT, MiningFee(1)
        sigmaProp(allOf(
            Coll(
                _OwnerPK,
                isNFTReturned
            )
        ))
    }
}