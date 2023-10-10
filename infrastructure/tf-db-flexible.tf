locals {
  db_host_name = "flexible-${var.product}-${var.component}"
}

module "postgresql" {
  providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }

  source               = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  name                 = local.db_host_name
  product              = var.product
  component            = var.component
  location             = var.location
  env                  = var.env
  pgsql_admin_username = local.postgresql_user
  pgsql_databases = [
    {
      name : local.db_name
    }
  ]
  common_tags   = var.common_tags
  business_area = "sds"
  pgsql_version = "15"

  pgsql_server_configuration = [
    {
      name  = "azure.extensions"
      value = "plpgsql, pg_stat_statements, pg_buffercache"
    }
  ]

  admin_user_object_id = var.jenkins_AAD_objectId
}

# SDP access and MV required in here. Will be done at migration
resource "postgresql_role" "create_sdp_access" {
  provider = "postgresql.postgres-flexible"

  name                = data.azurerm_key_vault_secret.sdp-user.value
  login               = true
  password            = data.azurerm_key_vault_secret.sdp-pass.value
  skip_reassign_owned = true
  skip_drop_role      = true
  count               = var.env == "sbox" ? 1 : 0
}

resource "postgresql_grant" "readonly_mv" {
  provider = "postgresql.postgres-flexible"

  database    = module.postgresql.postgresql_database
  role        = data.azurerm_key_vault_secret.sdp-user.value
  schema      = "public"
  object_type = "table"
  privileges  = ["SELECT"]
  objects     = ["sdp_mat_view_pi_user"]
  count       = var.env == "sbox" ? 1 : 0
}
