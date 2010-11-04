<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- begin.jsp -->
<html:xhtml/>

<!-- metabolicMine CSS -->
<!--[if lt IE 8]><link rel="stylesheet" href="/metabolicmine/model/public/css/ie.css" type="text/css" media="screen, projection"/><![endif]-->

<div id="corner">&nbsp;</div>
<div id="help"><a href="#" onclick="showContactForm();return false;" title="Need help?">&nbsp;</a></div>

<script type="text/javascript">
	// specify what happens to element in a small browser window (better add class than modify from here)
	if ($(window).width() < '1205') {
		$('#corner').addClass('smallScreen');
		if ($(window).width() < '1125') {
			$('#help').addClass('smallScreen');
		}
	}
	// placeholder value for search boxes
	var placeholder = 'e.g. PPARG, Insulin, rs876498';
	// class used when toggling placeholder
	var inputToggleClass = 'eg';
</script>

<!-- BluePrint CSS container -->
<div class="container">

	<div id="welcome" class="span-12 last wide-blue">
		<a class="close" href="#" title="Close" onclick="javascript:jQuery('#welcome').slideUp();return false;">&nbsp;</a>
        <div class="top"></div>
        <div class="center span-12 last">
        	<div class="bochs" id="bochs-1">
	            <div id="thumb" class="span-4">
	                <img src="/metabolicmine/themes/metabolic/thumbs/thumb-image.png" alt="metabolicMine interface" />
	            </div>
	            <div class="span-8 last">
			        <h2>First time here?</h2>
			        <p>Welcome to metabolicMine a place to explore the relation between genes and <u>metabolic diseases</u>.
			        	Check out lorem ipsum dolor sit if you want to do a tour element sit. You can create a <strong>MyMine
			            account</strong> and log in to save queries and lists permanently.<br />Dolor sit element rex pitus
			            grgalis relation if you lorem.</p>
			        <br />
			        <a class="button blue" href="<c:url value="/help/begin.html" />"
			        		onclick="javascript:window.open('<c:url value="/help/begin.html" />','_help','toolbar=0,scrollbars=1,location=1,statusbar=1,menubar=0,resizable=1,width=800,height=600');return false">
			        			<div><span>Take a tour</span></div>
					</a>
					<a class="button gray" href="#" onclick="showContactForm();return false;">
			        	<div><span>Contact team</span></div>
					</a>
	            </div>
            </div>
            <div class="bochs" id="bochs-2" style="display:none;">
	            <div id="thumb" class="span-4">
	                <img src="/metabolicmine/themes/metabolic/thumbs/thumb-image.png" alt="metabolicMine interface" />
	            </div>
	            <div class="span-8 last">
			        <h2>More text goes here</h2>
			        <p>Lorem ipsum dolor sit.</p>
			        <br />
			        <a class="button gray" href="#" onclick="showContactForm();return false;">
			        	<div><span>Something goes here</span></div>
					</a>
	            </div>
            </div>
            <div class="span-12 last">
            	<ul id="switcher">
            	<!--
            		<li id="switcher-1" class="switcher current"><a onclick="switchBochs(1);return false;" href="#">1</a></li>
            		<li id="switcher-2" class="switcher"><a onclick="switchBochs(2);return false;" href="#">2</a></li>
            	-->
            	</ul>
            </div>
        </div>
        <div class="bottom span-12 last"></div>
    </div>
    
    <!--
    <script type="text/javascript">
		/* div switcher for welcome bochs using jQuery */
		function switchBochs(newDivId) {
			// no current
			javascript:jQuery(".switcher").each (function() { javascript:jQuery(this).removeClass('current'); });
			// apply current
			javascript:jQuery('#switcher-'+newDivId).addClass('current');
			// hide them all bochs
			javascript:jQuery(".bochs").each (function() { javascript:jQuery(this).hide(); });
			// then show our baby
			javascript:jQuery('#bochs-'+newDivId).fadeIn();
		}
	</script>
	-->

    <div id="actions" class="span-12 last wide-gray">
    
    	<form action="<c:url value="/keywordSearchResults.do" />" name="search" method="get">
	        <!--
	        <a class="more" title="Want to see more?"
	           onclick="javascript:jQuery('#more-actions').slideToggle(); return false;">&nbsp;</a>
	        -->
	        <div class="top"></div>
	        <div class="center span-12 last">
	            <div class="span-4 search">
		            <script type="text/javascript">
		            	/* pre-fill search input with a term */
		            	function preFillInput(term) {
		            		var e = $("input#actionsInput");
		            		e.val(term);
							if (e.hasClass(inputToggleClass)) e.toggleClass(inputToggleClass);
							e.focus();
		            	}
		            </script>
	            	<div class="image">
	                	<img src="images/icons/search-64.png" alt="Search" />
	                </div>
	                <h3>Search for gene identifiers</h3>
	                <div style="clear:both;"> </div>
	                <p>Enter a gene, protein, SNP or other identifier [eg.
	                <a onclick="preFillInput('PPARG');return false;" title="Search for PPARG"
	                	href="keywordSearchResults.do?searchTerm=PPARG"><strong>PPARG</strong></a>,
	                <a onclick="preFillInput('Insulin');return false;" title="Search for Insulin"
	                	href="keywordSearchResults.do?searchTerm=Insulin"><strong>Insulin</strong></a>,
	                <a onclick="preFillInput('rs876498');return false;" title="Search for rs876498"
	                	href="keywordSearchResults.do?searchTerm=rs876498"><strong>rs876498</strong></a>].
	                <br />Alternatively, search for disease, keywords or publications [eg.
	                <a onclick="preFillInput('Diabetes');return false;" title="Search for Diabetes"
	                	href="keywordSearchResults.do?searchTerm=Diabetes"><strong>Diabetes</strong></a>,
	                <a onclick="preFillInput('GWAS');return false;" title="Search for GWAS"
	                	href="keywordSearchResults.do?searchTerm=GWAS"><strong>GWAS</strong></a>,
	                <a onclick="preFillInput('Nature');return false;" title="Search for Nature"
	                	href="keywordSearchResults.do?searchTerm=Nature"><strong>Nature</strong></a>
	                ]</p>
	                <p>[Supports Boolean and wildcard*]</p>
						<div class="input">
							<input id="actionsInput" class="input" type="text" name="searchTerm" value="e.g. PPARG, Insulin, rs876498" />
						</div>
	            </div>
	            <div class="span-4 lists">
	            	<div class="image">
	                	<img src="images/icons/lists-64.png" alt="Lists" />
	                </div>
	                <h3>Query lists of data</h3>
	                <div style="clear:both;"> </div>
					<p>
					<img src="/metabolicmine/themes/metabolic/thumbs/widget-charts-3.png" alt="widget charts" style="float:right;padding-left:5px;margin-right:10px;" />
					<strong>Explore</strong>, <strong>Analyse</strong> &amp; <strong>Enrich</strong>. Upload your own data or browse our Public
					sets.Covering Pathways to Publications, Search for hidden relationships with our <strong>widgets library</strong>.</p>
	            </div>
	            <div class="span-4 last templates">
	                <div class="image">
	                	<img src="images/icons/templates-64.png" alt="Templates" />
	                </div>
	                <h3>Work with template queries</h3>
	                <div style="clear:both;"> </div>
	                <p>Get started with complex queries using our <strong>pre-defined searches</strong>. These customizable templates have been
	                designed around common tasks performed by our <strong>biologist community</strong>.To see how they work, why not try a
	                template from our examples page?</p>
	            </div>
	            <div class="span-12 last">
	                <div class="span-4 search">
	                    <input class="button orange-gray" type="submit" value="Search" />
	                </div>
	                <div class="span-4 lists">
	                    <a href="/${WEB_PROPERTIES['webapp.path']}/bag.do?subtab=view" class="button green">
	                    	<div><span>Lists</span></div>
	                    </a>
	                </div>
	                <div class="span-4 last templates">
	                    <a href="/${WEB_PROPERTIES['webapp.path']}/templates.do" class="button violet">
	                    	<div><span>Templates</span></div>
	                    </a>
	                </div>
	            </div>
        	</div>
        	<div class="bottom"></div>
        </form>
    </div>

    <div id="api" class="span-6 white-half">
        <div class="top"></div>
        <div class="center span-6 last">
            <h4>Perl<span>&nbsp;&amp;&nbsp;</span>Java API</h4>
            <img src="/metabolicmine/themes/metabolic/icons/perl-java-ico.gif" alt="perl java" />
            <p>We support programatic access to our data through Application Programming Interface too! Choose from options below:</p>
            <ul>
                <li><a href="#">Perl API</a></li>
                <li><a href="#">Java API</a></li>
            </ul>
        </div>
        <div class="bottom span-6 last"></div>
    </div>
    
    <div id="rss" class="span-6 last white-half" style="display:none;">
    	<script type="text/javascript">
			// feed URL
			var feedURL = "http://blog.metabolicmine.org/?feed=rss2";
			// limit number of entries displayed
			var maxEntries = 2;
			// where are we appending entries? (jQuery syntax)
			var target = 'table#articles';

			var months = new Array(12); months[0]="Jan"; months[1]="Feb"; months[2]="Mar"; months[3]="Apr"; months[4]="May"; months[5]="Jun";
			months[6]="Jul"; months[7]="Aug"; months[8]="Sep"; months[9]="Oct"; months[10]="Nov"; months[11]="Dec";

			$(document).ready(function() {
				// DWR fetch, see AjaxServices.java
				AjaxServices.getNewsPreview(feedURL, function(data) {
					if (data) {
						// show us
						$('#rss').slideToggle('slow');
					
						// declare
						var feedTitle, feedDescription, feedDate, feedLink, row;
	
						// convert to XML, jQuery manky...
			            var feed = new DOMParser().parseFromString(data, "text/xml");
			            var items = feed.getElementsByTagName("item"); // ATOM!!!
			            for (var i = 0; i < items.length; ++i) {
							// early bath
							if (i > maxEntries) return;
	
				            feedTitle = trimmer(items[i].getElementsByTagName("title")[0].firstChild.nodeValue, 70);
				            feedDescription = trimmer(items[i].getElementsByTagName("description")[0].firstChild.nodeValue, 70);
				            feedDate = new Date(items[i].getElementsByTagName("pubDate")[0].firstChild.nodeValue);
				            feedLink = items[i].getElementsByTagName("link")[0].firstChild.nodeValue
	
	    					// build table row
	    					row = '<tr>'
	        	                    + '<td class="date">'
	        	                    	+ '<a target="new" href="' + feedLink + '">' + feedDate.getDate()
	        	                    	+ '<br /><span>' + months[feedDate.getMonth()] + '</span></a></td>'
	        	                    + '<td><a target="new" href="' + feedLink + '">' + feedTitle + '</a><br/>' + feedDescription + '</td>'
	    	                	+ '</tr>';
	    					// append, done
	    					$(target).append(row);
	    					i++;
		            	}
		            } 
				});
			});
        	
        	// trim text to a specified length
			function trimmer(grass, length) {
				if (!grass) return;
				grass = stripHTML(grass);
				if (grass.length > length) return grass.substring(0, length) + '...';
				return grass;
			}

			// strip HTML
        	function stripHTML(html) {
        	   var tmp = document.createElement("DIV"); tmp.innerHTML = html; return tmp.textContent || tmp.innerText;
        	}
    	</script>
        <div class="top"></div>
        <div class="center span-6 last">
            <h4>News<span>&nbsp;&amp;&nbsp;</span>Updates</h4>
            <table id="articles">
            	<!-- append babies here -->
            </table>
        </div>
        <div class="more span-6 last"><a target="new" href="http://blog.metabolicmine.org/" class="more">&nbsp;</a></div>
    </div>
    
    <!-- 
    <div id="testimonials" class="span-4 last blue">
        <div class="top"></div>
        <div class="center span-4 last">
            <h4>User Feedback</h4>
            <img src="/metabolicmine/themes/metabolic/img/stockphoto.jpg" alt="Stock Photo" />
            <p>It's not how fat you are, it's what you do with it that counts, and that is why we feel that
                metabolicMine is a valid addition to the data mining lorem.</p>
            <q>- E. Novak-Brown, University of Cambridge</q>
        </div>
        <div class="bottom span-4 last"></div>
    </div>
    -->

    <div id="footer" class="span-12 last">
        <div class="span-6">
            <a href="#">Contact Us</a>
            <span>|</span>
            <a href="#">Privacy Policy</a>
            <span>|</span>
            <a href="#">Section 508 Statement</a>
            <br />
            <a href="#">ModMine</a>
            <span>|</span>
            <a href="#">InterMine</a>
            <span>|</span>
            <a href="#">FlyMine</a>
            <span>|</span>
            <a href="#">MalariaMine</a>
            <span>|</span>
            <a href="#">GoldMine</a>
            
            <p>&copy; 2010 Department of Genetics, University of Cambridge, Downing Street, Cambridge CB2 3EH, United Kingdom</p>
        </div>
        <div class="span-6 last">
            <a href="http://wellcome.ac.uk/" title="Wellcome Trust">
            	<img src="/metabolicmine/themes/metabolic/icons/wellcome-ico.png" alt="Wellcome Trust" />
           	</a>
           	<a href="http://www.gen.cam.ac.uk/" title="Department of Genetics">
           		<img src="/metabolicmine/themes/metabolic/icons/genetics-ico.gif" alt="Department of Genetics" />
           	</a>
           	<a href="http://www.cam.ac.uk/" title="University of Cambridge">
            	<img src="/metabolicmine/themes/metabolic/icons/cam-ico.gif" alt="University of Cambridge" />
            </a>
        </div>
    </div>

 </div>
 
 <script type="text/javascript">
 	// e.g. values only available when JavaScript is on
	$('input#actionsInput').toggleClass(inputToggleClass);

	// register input elements with blur & focus
	$('input#actionsInput').blur(function() {
		if ($(this).val() == '') {
			$(this).toggleClass(inputToggleClass);
			$(this).val(placeholder);
		}
	});
	$('input#actionsInput').focus(function() {
		if ($(this).hasClass(inputToggleClass)) {
			$(this).toggleClass(inputToggleClass);
			$(this).val('');
		}
	});
</script>