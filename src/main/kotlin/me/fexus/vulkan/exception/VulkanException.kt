package me.fexus.vulkan.exception


/**
 * If this happens then it'll probably be 99.99% the programmers (my) fault.
 * The 0.01 % will either be because someone tampered with the program or because someone DIDN'T UPDATE THEIR DRIVERS HOW OFTEN DO I HAVE TO WRITE THIS IN HERE LIKE BRUH FRFRFR.
 */

class VulkanException(code: Int) : Exception("Exception error code ${getVulkanErrorMessage(code)}"){
    companion object {
        private fun getVulkanErrorMessage(code: Int) = buildString {
            val result = Result.values().firstOrNull { it.vkID == code }
            if(result != null) {
                append("$code: ${result.name}")
            } else {
                append("$code: [UNKNOWN ERROR CODE]")
            }
        }
    }
}