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
    // Inputs: ProfileBox(0)
    // Outputs: ProfileBox(0), MiningBox(1)
    val isProfileBoxOutput: Boolean = allOf(Coll(
        inProfileBox.tokens(0)._1 == outProfileBox.tokens(0)._1,
        inProfileBox.tokens(0)._2 == outProfileBox.tokens(0)._2,
        inProfileBox.tokens(0)._1 == _DumDumDumProfileToken,
        inProfileBox.R4[Coll[Byte]] == outProfileBox.R4[Coll[Byte]]
    ))

    if (isProfileBoxOutput) {
        val dataInputBox: Box           = CONTEXT.dataInputs(0)

        val isProfileBoxTokenSame: Boolean = allOf(Coll(
            inProfileBox.tokens(0)._1 == outProfileBox.tokens(0)._1,
            inProfileBox.tokens(0)._2 == outProfileBox.tokens(0)._2,
            inProfileBox.tokens.size == 1,
            outProfileBox.tokens.size == 1,
            inProfileBox.tokens(0)._1 == _DumDumDumProfileToken,
            inProfileBox.R4[Coll[Byte]] == outProfileBox.R4[Coll[Byte]]
        ))

        val isProfileBoxValueSame: Boolean =
            inProfileBox.value == outProfileBox.value

        val isProfileBoxAddressSame: Boolean =
            inProfileBox.propositionBytes == outProfileBox.propositionBytes

        // NFT Change
        val isNftDataInputBelongsToUser: Boolean =
            dataInputBox.propositionBytes == inProfileBox.R4[Coll[Byte]].get

        val filteredNFTToken: Coll[(Coll[Byte], Long)] =
            dataInputBox.tokens
                .filter{ (token: (Coll[Byte], Long)) => token._1 == outProfileBox.R5[Coll[Byte]].get}

        val isNftInDataInputBox: Boolean =
            filteredNFTToken.size == 1

        val isNftChange: Boolean = allOf(Coll(
            isNftDataInputBelongsToUser,
            isNftInDataInputBox
        ))

        sigmaProp(allOf(
            Coll(
                _OwnerPK,
                anyOf(Coll(
                    isNftChange,
                    inProfileBox.R6[Coll[Coll[Byte]]] == outProfileBox.R6[Coll[Coll[Byte]]]
                )),
                isProfileBoxTokenSame,
                isProfileBoxValueSame,
                isProfileBoxAddressSame
            )
        ))
    } else if (OUTPUTS.size == 1) {
        // ====== Deletion ====== //
        // Inputs: ProfileBox(0)
        // Outputs: MiningBox(0)
        _OwnerPK
    } else {
        sigmaProp(false)
    }
}