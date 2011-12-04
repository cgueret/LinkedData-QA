<xsl:stylesheet version='2.0'
	xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
	
	<xsl:output method="text" version="1.0" encoding="UTF-8" indent="no" />

	<xsl:variable name="newline">
		<xsl:text>&#xa;</xsl:text>
	</xsl:variable>


	<xsl:template match="/html">

		{		
		"metricStatus": {
		<xsl:for-each select="./table[1]">
			<xsl:for-each select="./tr[./td]">
				"<xsl:value-of select="./td[1]" />"
				: { "status":
				"<xsl:value-of select="normalize-space(./td[2])" />"
				, "change":
				<xsl:value-of select="number(replace(./td[3], '%', '')) div 100.0" />
				}
				<xsl:if test="not(position() = last())">, </xsl:if>
			</xsl:for-each>
			}
		</xsl:for-each>


		, "outliers": {
		<xsl:for-each select="./table[2]">
			<xsl:for-each select="./tr[./td]">
				"<xsl:value-of select="./td[1]" />"
				: {
				<xsl:for-each select="./td[position() >= 2]">
					<xsl:analyze-string regex="([^)]*)\(([^)]*)\)"
						flags="ix" select=".">
						<xsl:matching-substring>
							"<xsl:value-of select="normalize-space(regex-group(1))" />"
							:
							<xsl:value-of select="regex-group(2)" />
						</xsl:matching-substring>
					</xsl:analyze-string>
					<xsl:if test="not(position() = last())">, </xsl:if>
				</xsl:for-each>
				}
				<xsl:if test="not(position() = last())">, </xsl:if>
			</xsl:for-each>
		</xsl:for-each>
		}
		}

	</xsl:template>
</xsl:stylesheet>

