module.exports.processTransaction = function(params, success, fail) {
  cordova.exec(success, fail, "Paymill", "processTransaction", [
    params.cardholder,
    params.number,
    params.exp_month,
    params.exp_year,
    params.cvc,
    params.currency,
    params.amount_int
  ]);
};
