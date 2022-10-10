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
    // val _DumDumDumToken:                 Coll[Byte]

    // ===== Box Details ===== //
    // R4: Profile Picture (NFT Token Id) => Coll[Byte]
    // R5: Following (WalletAddress) => AVLTree

    // ===== Contract Conditions ===== //
    // 1. Adding/Removing Following
    // 2. Change Profile Picture

    val _minFee: Long       = 1000 * 1000
    val inProfileBox: Box   = INPUTS(0)
    val outProfileBox: Box  = OUTPUTS(0)

    // ### Modifying Profile
    if (OUTPUTS.size == 2) {
        sigmaProp(allOf(
            Coll(
                _OwnerPk,
                anyOf(
                    inProfileBox.R4[Coll[Byte]] == outProfileBox.R4[Coll[Byte]],
                    inProfileBox.R5[AvlTree] == outProfileBox.R5[AvlTree]
                ),
                inProfileBox.tokens(0)._1 == outProfileBox.tokens(0)._1,
                inProfileBox.tokens(0)._2 == outProfileBox.tokens(0)._2,
                inProfileBox.tokens.size == 1,
                outProfileBox.tokens.size == 1,
                inProfileBox.tokens(0)._1 == _DumDumDumToken,
                inProfileBox.value == outProfileBox.value
            )
        ))
    } else if (OUTPUTS.size == 1) {
        // ### Deleting Profile Box
        _OwnerPK
    } else {
        sigmaProp(false)
    }
}