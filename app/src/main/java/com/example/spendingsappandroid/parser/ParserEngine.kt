package com.example.spendingsappandroid.parser

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Structured result from parsing a financial notification.
 * Only structured data is retained — raw notification text is never stored.
 */
data class ParseResult(
    val amount: Double?,
    val currency: String?,
    val merchant: String?,
    val confidence: Float
)

/**
 * Interface for app-specific transaction parsers.
 * Implement this to add support for parsing notifications from a specific financial app.
 */
interface AppNotificationParser {
    /** Package names this parser handles. Empty set means it handles all packages (generic). */
    val supportedPackages: Set<String>

    /** Attempt to parse transaction data from notification title and text. */
    fun parse(title: String, text: String): ParseResult?
}

/**
 * Orchestrates parsing of financial notifications using a chain of parsers.
 * App-specific parsers are tried first; falls back to the generic parser.
 * Designed so new parsers per app can be registered at any time.
 */
@Singleton
class ParserEngine @Inject constructor() {

    private val parsers = mutableListOf<AppNotificationParser>()

    init {
        // Register the default generic parser.
        // To add app-specific parsers: parsers.add(0, MyBankParser())
        parsers.add(GenericTransactionParser())
    }

    /** Register an additional app-specific parser. It takes priority over existing ones. */
    fun registerParser(parser: AppNotificationParser) {
        parsers.add(0, parser)
    }

    /**
     * Attempts to parse transaction data from a notification.
     * Returns null if no parser produces a result above [CONFIDENCE_THRESHOLD].
     */
    fun parse(packageName: String, title: String, text: String): ParseResult? {
        for (parser in parsers) {
            if (parser.supportedPackages.isEmpty() || packageName in parser.supportedPackages) {
                val result = parser.parse(title, text)
                if (result != null && result.confidence >= CONFIDENCE_THRESHOLD) {
                    return result
                }
            }
        }
        return null
    }

    companion object {
        const val CONFIDENCE_THRESHOLD = 0.8f
    }
}

/**
 * Generic parser that uses regex patterns to extract transaction data
 * from any financial notification text. Handles multiple currency formats
 * and decimal separator conventions.
 */
class GenericTransactionParser : AppNotificationParser {

    override val supportedPackages: Set<String> = emptySet() // Matches all

    // --- Currency symbol → ISO code mapping ---
    private val currencySymbolMap = mapOf(
        "$" to "USD", "€" to "EUR", "£" to "GBP",
        "¥" to "JPY", "₹" to "INR", "₩" to "KRW",
        "₽" to "RUB", "R$" to "BRL", "₺" to "TRY",
        "₱" to "PHP", "฿" to "THB", "zł" to "PLN"
    )

    private val isoCodePattern = Regex(
        "(USD|EUR|GBP|JPY|INR|CAD|AUD|CHF|CNY|KRW|BRL|TRY|RUB|PHP|THB|PLN|SGD|HKD|MXN|ZAR)",
        RegexOption.IGNORE_CASE
    )

    // --- Amount extraction patterns (ordered by specificity) ---
    private val amountPatterns = listOf(
        // Symbol before amount: $1,234.56 or €1.234,56
        Regex("""([\$€£¥₹₩₽₺₱฿]|R\$|zł)\s?(\d{1,3}(?:[,.\s]\d{3})*(?:[.,]\d{1,2})?)"""),
        // Keyword + amount: "spent 1234.56" or "amount: $1234.56"
        Regex(
            """(?:amount|spent|charged|paid|debited|credited|purchase|total)\s*:?\s*(?:[\$€£¥₹₩₽₺₱฿]|R\$|zł)?\s*(\d{1,3}(?:[,.\s]\d{3})*(?:[.,]\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        ),
        // Amount + ISO code: "1234.56 USD"
        Regex(
            """(\d{1,3}(?:[,.\s]\d{3})*(?:[.,]\d{1,2})?)\s*(USD|EUR|GBP|JPY|INR|CAD|AUD|CHF|CNY)""",
            RegexOption.IGNORE_CASE
        ),
        // Standalone decimal number: "1234.56" or "1,234.56" (lowest priority)
        Regex("""(\d{1,3}(?:,\d{3})*\.\d{2})""")
    )

    // --- Merchant extraction patterns ---
    private val merchantPatterns = listOf(
        Regex(
            """(?:at|to|from|@)\s+([A-Za-z0-9][\w\s&'.,-]{1,40}?)(?:\s+(?:on|for|ref|txn|transaction|via|using)|[.]|$)""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """(?:merchant|store|shop|vendor|payee)\s*:?\s*([A-Za-z0-9][\w\s&'.,-]{1,40}?)(?:\s+(?:on|for|ref)|[.]|$)""",
            RegexOption.IGNORE_CASE
        )
    )

    override fun parse(title: String, text: String): ParseResult {
        val combined = "$title $text"

        val (amount, symbolCurrency) = extractAmount(combined)
        val currency = symbolCurrency ?: extractCurrencyISO(combined)
        val merchant = extractMerchant(combined)

        val confidence = calculateConfidence(amount, currency, merchant)

        return ParseResult(
            amount = amount,
            currency = currency,
            merchant = merchant,
            confidence = confidence
        )
    }

    private fun extractAmount(text: String): Pair<Double?, String?> {
        for (pattern in amountPatterns) {
            val match = pattern.find(text) ?: continue
            return when (match.groupValues.size) {
                3 -> {
                    val g1 = match.groupValues[1]
                    val g2 = match.groupValues[2]
                    val symbol = currencySymbolMap[g1]
                    if (symbol != null) {
                        // (symbol, amount)
                        normalizeAmount(g2) to symbol
                    } else {
                        // (amount, isoCode) or (keyword-captured amount, -)
                        val iso = if (g2.length == 3 && g2.all { it.isLetter() }) g2.uppercase() else null
                        normalizeAmount(g1) to iso
                    }
                }
                2 -> normalizeAmount(match.groupValues[1]) to null
                else -> continue
            }
        }
        return null to null
    }

    private fun extractCurrencyISO(text: String): String? {
        return isoCodePattern.find(text)?.value?.uppercase()
    }

    private fun extractMerchant(text: String): String? {
        for (pattern in merchantPatterns) {
            val match = pattern.find(text) ?: continue
            val raw = match.groupValues[1].trim()
            if (raw.isNotBlank()) return cleanMerchantName(raw)
        }
        return null
    }

    /**
     * Normalize decimal separators.
     * Handles "1.234,56" (EU format) and "1,234.56" (US format).
     */
    private fun normalizeAmount(raw: String): Double? {
        if (raw.isBlank()) return null
        var cleaned = raw.replace(Regex("""\s"""), "")

        val lastDot = cleaned.lastIndexOf('.')
        val lastComma = cleaned.lastIndexOf(',')

        cleaned = when {
            lastDot > lastComma -> cleaned.replace(",", "")                                   // US: 1,234.56
            lastComma > lastDot -> cleaned.replace(".", "").replace(",", ".")                  // EU: 1.234,56
            lastComma != -1 && lastDot == -1 -> {
                val afterComma = cleaned.substringAfter(',')
                if (afterComma.length <= 2) cleaned.replace(",", ".")  // Decimal: 12,50
                else cleaned.replace(",", "")                          // Thousands: 1,234
            }
            else -> cleaned
        }
        return cleaned.toDoubleOrNull()
    }

    /** Clean up merchant name by removing noise characters. */
    private fun cleanMerchantName(raw: String): String {
        return raw
            .replace(Regex("""[*#]+"""), "")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
            .replaceFirstChar { it.uppercase() }
    }

    /**
     * Calculate confidence score based on extracted fields.
     * Amount is required; each additional field increases confidence.
     * Threshold of 0.8 requires amount + at least one other field.
     */
    private fun calculateConfidence(amount: Double?, currency: String?, merchant: String?): Float {
        if (amount == null) return 0.0f
        var score = 0.6f // Amount found
        if (currency != null) score += 0.2f
        if (merchant != null) score += 0.2f
        return score
    }
}
