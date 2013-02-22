/**
Open Bank Project - Transparency / Social Finance Web Application
Copyright (C) 2011, 2012, TESOBE / Music Pictures Ltd

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Email: contact@tesobe.com
TESOBE / Music Pictures Ltd
Osloerstrasse 16/17
Berlin 13359, Germany

  This product includes software developed at
  TESOBE (http://www.tesobe.com/)
  by
  Simon Redfern : simon AT tesobe DOT com
  Stefan Bethge : stefan AT tesobe DOT com
  Everett Sochowski : everett AT tesobe DOT com
  Ayoub Benali: ayoub AT tesobe DOT com

 */

package code.api

import net.liftweb.common.{Box,Full,Loggable}
import net.liftweb.mapper.By
import net.liftweb.http.S
import net.liftweb.http.rest.RestHelper
import net.liftweb.util.Props
import net.liftweb.http.JsonResponse
import net.liftweb.json.Extraction
import net.liftweb.json.JsonAST.JValue
import code.model.Token

case class ErrorMessage(
  error : String
)
case class TokenValidity(
  isValid : Boolean
)

/**
* this object provides the API call required for the Bank mock,
* They are required during the User authentication / Application Authorization steps
* that the Bank Mock need to handle as part of the OAuth 1.0 protocol authentication.
*/
object BankMockAPI extends RestHelper with Loggable {

  implicit def errorToJson(error: ErrorMessage): JValue = Extraction.decompose(error)
  implicit def errorToJson(msg: TokenValidity): JValue = Extraction.decompose(msg)

  //extract and compare the sent key with the local one (shared secret)
  def isValidKey : Boolean = {
    val sentKey : Box[String] =
      for{
        req <- S.request
        sentKey <- req.header("BankMockKey")
      } yield sentKey
    val localKey : Box[String] = Props.get("BankMockKey")
    localKey == sentKey
  }
  serve("obp" / "v1.0" prefix {
    case "token-verification" :: Nil JsonGet _ => {
      if(isValidKey)
        S.param("oauth_token") match {
          case Full(token) => {
            Token.find(By(Token.key,token)) match {
              case Full(tkn) => JsonResponse(TokenValidity(tkn.isValid), Nil, Nil, 200)
              case _ => JsonResponse(ErrorMessage("invalid or expired OAuth token"), Nil, Nil, 401)
            }
          }
          case _ => JsonResponse(ErrorMessage("No OAuth token"), Nil, Nil, 401)
        }
      else
        JsonResponse(ErrorMessage("No key found or wrong key"), Nil, Nil, 401)
    }
  })
}