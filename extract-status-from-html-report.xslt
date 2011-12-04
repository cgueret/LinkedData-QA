<xsl:stylesheet version='2.0'
	xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
	
	<xsl:output method="text" version="1.0" encoding="UTF-8" indent="no" />

	<xsl:variable name="newline">
		<xsl:text>&#xa;</xsl:text>
	</xsl:variable>


	<xsl:template match="/html">

		<xsl:for-each select="./table[1]">
			<xsl:for-each select="./tr[./td]">
				<xsl:value-of select="replace(./td[1], ' ', '_')" /><xsl:text> </xsl:text>
				<xsl:value-of select="normalize-space(./td[2])" /><xsl:text> </xsl:text>
				<xsl:value-of select="number(replace(./td[3], '%', '')) div 100.0" />
				<xsl:value-of select="$newline" />
			</xsl:for-each>
		</xsl:for-each>

	</xsl:template>
</xsl:stylesheet>

