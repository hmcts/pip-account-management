terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      version = "3.14"
    }
  }
}

provider "azurerm" {
  features {}
}

