provider "azurerm" {}

locals {
  ase_name               = "${data.terraform_remote_state.core_apps_compute.ase_name[0]}"
  vaultName              = "${var.product}-${var.env}"
  asp_name               = "${var.product}-${var.env}"
}

data "azurerm_key_vault" "workallocation_key_vault" {
  name = "${local.vaultName}"
  resource_group_name = "${var.product}-${var.env}"
}

data "azurerm_key_vault_secret" "s2s_secret" {
  name      = "CTSC-S2S-SECRET"
  vault_uri = "${data.azurerm_key_vault.workallocation_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "service_user_password" {
  name      = "CTSC-SERVICE-USER-PASSWORD"
  vault_uri = "${data.azurerm_key_vault.workallocation_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "service_user_email" {
  name      = "CTSC-SERVICE-USER-EMAIL"
  vault_uri = "${data.azurerm_key_vault.workallocation_key_vault.vault_uri}"
}

# Make sure the resource group exists
resource "azurerm_resource_group" "rg" {
  name     = "${local.asp_name}"
  location = "${var.location_app}"
}

module "servicebus-namespace" {
  source                = "git@github.com:hmcts/terraform-module-servicebus-namespace.git"
  name                  = "${var.product}-servicebus-${var.env}"
  location              = "${var.location_app}"
  resource_group_name   = "${azurerm_resource_group.rg.name}"
  common_tags           = "${var.common_tags}"
  env                   = "${var.env}"
}

module "work-allocation-queue" {
  source = "git@github.com:hmcts/terraform-module-servicebus-queue.git"
  name = "${var.product}-work-allocation-queue-${var.env}"
  namespace_name = "${module.servicebus-namespace.name}"
  resource_group_name = "${azurerm_resource_group.rg.name}"
}

module "ctsc-work-allocation" {
  source              = "git@github.com:hmcts/cnp-module-webapp?ref=master"
  product             = "${var.product}-${var.component}"
  location            = "${var.location_app}"
  env                 = "${var.env}"
  ilbIp               = "${var.ilbIp}"
  subscription        = "${var.subscription}"
  capacity            = "${var.capacity}"
  common_tags         = "${var.common_tags}"
  asp_name            = "${local.asp_name}"
  asp_rg              = "${local.asp_name}"

  app_settings = {
    LOGBACK_REQUIRE_ALERT_LEVEL = "false"
    LOGBACK_REQUIRE_ERROR_CODE  = "false"
    S2S_SECRET = "${data.azurerm_key_vault_secret.s2s_secret.value}"
    S2S_AUTH_URL = "http://${var.idam_s2s_url_prefix}-${var.env}.service.${local.ase_name}.internal"
    SERVER_URL = "http://${var.ctsc_server_url_prefix}-${var.env}.service.${local.ase_name}.internal"
    CCD_API_URL = "http://${var.ccd_api_url_prefix}-${var.env}.service.${local.ase_name}.internal"
    IDAM_CLIENT_BASE_URL = "${var.idam_api_url}"
    LAST_RUN_LOG = "${var.last_run_log_file}"
    SERVICE_USER_EMAIL = "${data.azurerm_key_vault_secret.service_user_email.value}"
    SERVICE_USER_PASSWORD = "${data.azurerm_key_vault_secret.service_user_password.value}"
  }
}

