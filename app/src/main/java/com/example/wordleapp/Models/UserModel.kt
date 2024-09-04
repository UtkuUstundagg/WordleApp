package com.example.wordleapp.Models

import android.content.BroadcastReceiver
import java.io.Serializable

class UserModel(
    var id: String? = null,
    var username: String? = null,
    var password: String? =null,
    var status: String? =null): Serializable

class GameInviteModel(var id: String? = null,
                      var senderId: String? = null,
                      var receiverId: String? = null,
                      var inviteStatus: String? = null)

class ResultPopupModel(var id: String? = null,
                       var username: String? = null,
                       var status: Boolean? = null,
                       var user1Point: PointModel? = null,
                       var user2Point: PointModel? = null)


class PointModel(var id: String? = null, var userId: String? = null, var username: String? = null, var point: Int? = null)

class RematchModel(var id:String? = null, var user1Id:String? = null, var user1Status: String? = null,
                                          var user2Id:String? = null, var user2Status: String? = null)

class GameActivityModel(var id: String? = null,
                        var inviteId: String? = null,
                        var gameStatus: String? = null,
                        var user1: UserModel? = null,
                        var user2: UserModel? = null,
                        var user1Guess: WordModel? = null,
                        var user2Guess: WordModel? = null,
                        var resultPopup: ResultPopupModel? = null,
                        var user1Point: PointModel? = null,
                        var user2Point: PointModel? = null,
                        var rematchPopup: RematchModel? = null)

class GuessModel4(var id: String? = null, var isApproved: String? = null,
                  var char1: String? = null, var char1Status: String? = null,
                  var char2: String? = null, var char2Status: String? = null,
                  var char3: String? = null, var char3Status: String? = null,
                  var char4: String? = null, var char4Status: String? = null)
class GuessModel5(var id: String? = null, var isApproved: String? = null,
                  var char1: String? = null, var char1Status: String? = null,
                  var char2: String? = null, var char2Status: String? = null,
                  var char3: String? = null, var char3Status: String? = null,
                  var char4: String? = null, var char4Status: String? = null,
                  var char5: String? = null, var char5Status: String? = null)
class GuessModel6(var id: String? = null, var isApproved: String? = null,
                  var char1: String? = null, var char1Status: String? = null,
                  var char2: String? = null, var char2Status: String? = null,
                  var char3: String? = null, var char3Status: String? = null,
                  var char4: String? = null, var char4Status: String? = null,
                  var char5: String? = null, var char5Status: String? = null,
                  var char6: String? = null, var char6Status: String? = null)
class GuessModel7(var id: String? = null, var isApproved: String? = null,
                  var char1: String? = null, var char1Status: String? = null,
                  var char2: String? = null, var char2Status: String? = null,
                  var char3: String? = null, var char3Status: String? = null,
                  var char4: String? = null, var char4Status: String? = null,
                  var char5: String? = null, var char5Status: String? = null,
                  var char6: String? = null, var char6Status: String? = null,
                  var char7: String? = null, var char7Status: String? = null)

class WordModel(var id: String? = null, var isApproved: String? = null,
                var char1: String? = null, var char1Status: String? = null,
                var char2: String? = null, var char2Status: String? = null,
                var char3: String? = null, var char3Status: String? = null,
                var char4: String? = null, var char4Status: String? = null,
                var char5: String? = null, var char5Status: String? = null,
                var char6: String? = null, var char6Status: String? = null,
                var char7: String? = null, var char7Status: String? = null)



