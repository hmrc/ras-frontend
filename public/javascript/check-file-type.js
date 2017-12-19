/*
* Copyright 2016 HM Revenue & Customs
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/


function checkFileType(form) {

    var arrInputs = form.getElementsByTagName("input");
    var input = arrInputs[0];
    var fileName = input.value;

    if (fileName.length > 0) {
       if (fileName.substr(fileName.length - ".csv".length, ".csv".length).toLowerCase() == ".csv") {
          return true;
       }else {
          $('.validation-summary').show();
          $('#error').html('Please upload a .csv file');
          $('#file-upload').addClass("form-field--error");
          $('#upload-error').empty();
          $('#upload-error').html('Please upload a .csv file');
          return false;
       }
    }
    else{
      $('.validation-summary').show();
      $('#error').html('Please select a file');
      $('#file-upload').addClass("form-field--error");
      $('#upload-error').empty();
      $('#upload-error').html('Please select a file');
      return false;
    }

}