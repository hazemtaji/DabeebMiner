package com.dabeeb.miner.parse.html;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dabeeb.miner.data.model.Outlink;
import com.dabeeb.miner.parse.html.DOMContentUtils.EndAddition.Format;
import com.dabeeb.miner.util.NodeWalker;

import org.apache.commons.configuration.Configuration;
import org.w3c.dom.*;

/**
 * A collection of methods for extracting content from DOM trees.
 * 
 * This class holds a few utility methods for pulling content out of DOM nodes,
 * such as getOutlinks, getText, etc.
 * 
 */
public class DOMContentUtils {

	public static final String[] videoExt = { ".flv", ".mp4", ".mpg", ".avi", ".asx" };
	private static final String[] imageExt = { ".jpg" };

	private static final Pattern swfobjectPattern = Pattern.compile("swfobject.embedSWF.[^;]*?Player[^;]*?file[^;]*?:\"([^\"]*?)\"[^;]*?:\"([^\"]*?\\.jpg)\"[^;]*?;", Pattern.MULTILINE);
	private static final Pattern displayNone = Pattern.compile("display:[^;]*?none", Pattern.CASE_INSENSITIVE);

	public static class LinkParams {
		public String elName;
		public String attrName;
		public int childLen;

		public LinkParams(String elName, String attrName, int childLen) {
			this.elName = elName;
			this.attrName = attrName;
			this.childLen = childLen;
		}

		public String toString() {
			return "LP[el=" + elName + ",attr=" + attrName + ",len=" + childLen + "]";
		}
	}

	private HashMap<String, LinkParams> linkParams = new HashMap<String, LinkParams>();

	public DOMContentUtils(Configuration conf) {
		// forceTags is used to override configurable tag ignoring, later on
		Collection<String> forceTags = new ArrayList<String>(1);

		linkParams.clear();
		linkParams.put("a", new LinkParams("a", "href", 1));
		linkParams.put("area", new LinkParams("area", "href", 0));
		/*if (conf.getBoolean("parser.html.form.use_action", true)) {
			linkParams.put("form", new LinkParams("form", "action", 1));
			if (conf.get("parser.html.form.use_action") != null) {
				forceTags.add("form");
			}
		}*/
		linkParams.put("frame", new LinkParams("frame", "src", 0));
		linkParams.put("iframe", new LinkParams("iframe", "src", 0));
		linkParams.put("script", new LinkParams("script", "src", 0));
		linkParams.put("link", new LinkParams("link", "href", 0));
		linkParams.put("img", new LinkParams("img", "src", 0));

		// remove unwanted link tags from the linkParams map
		String[] ignoreTags = conf.getStringArray("parser.html.outlinks.ignoreTags");
		for (int i = 0; ignoreTags != null && i < ignoreTags.length; i++) {
			if (!forceTags.contains(ignoreTags[i]))
				linkParams.remove(ignoreTags[i]);
		}
	}

	/**
	 * This method takes a {@link StringBuilder} and a DOM {@link Node}, and
	 * will append all the content text found beneath the DOM node to the
	 * <code>StringBuilder</code>.
	 * 
	 * <p>
	 * 
	 * If <code>abortOnNestedAnchors</code> is true, DOM traversal will be
	 * aborted and the <code>StringBuffer</code> will not contain any text
	 * encountered after a nested anchor is found.
	 * 
	 * <p>
	 * 
	 * @return true if nested anchors were found
	 */
	public boolean getText(StringBuilder sb, Node node, boolean abortOnNestedAnchors) {
		if (getTextHelper(sb, node, abortOnNestedAnchors, 0)) {
			return true;
		}
		return false;
	}

	/**
	 * This is a convinience method, equivalent to
	 * {@link #getText(StringBuffer,Node,boolean) getText(sb, node, false)}.
	 * 
	 */
	public void getText(StringBuilder sb, Node node) {
		getText(sb, node, false);
	}

	// returns true if abortOnNestedAnchors is true and we find nested
	// anchors
	private boolean getTextHelper(StringBuilder sb, Node node, boolean abortOnNestedAnchors, int anchorDepth) {
		boolean abort = false;
		NodeWalker walker = new NodeWalker(node);
		Stack<EndAddition> toCheckEnd = new Stack<EndAddition>();

		while (walker.hasNext()) {

			Node currentNode = walker.nextNode();
			String nodeName = currentNode.getNodeName();
			short nodeType = currentNode.getNodeType();

			if ("script".equalsIgnoreCase(nodeName)) {

				if (currentNode.getFirstChild() != null) {
					Matcher swfobjectMatcher = swfobjectPattern.matcher(currentNode.getFirstChild().getNodeValue());
					if (swfobjectMatcher.find()) {
						String videoFile = swfobjectMatcher.group(1);
						String thumbnailFile = swfobjectMatcher.group(2);
						sb.append("<video file=\"" + videoFile + "\" thumbnail=\"" + thumbnailFile + "\" />");
					}
				}

				walker.skipChildren();
			}
			if ("style".equalsIgnoreCase(nodeName)) {
				walker.skipChildren();
			}
			if (abortOnNestedAnchors && "a".equalsIgnoreCase(nodeName)) {
				anchorDepth++;
				if (anchorDepth > 1) {
					abort = true;
					break;
				}
			}
			if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
				Node styleAttr = currentNode.getAttributes().getNamedItem("style");
				if (styleAttr != null) {
					Matcher matcher = displayNone.matcher(styleAttr.getNodeValue());
					if (matcher.find())
						walker.skipChildren();
				}
			}

			if (nodeName.equalsIgnoreCase("table")) {
				sb.append("\n-{");
				toCheckEnd.add(new EndAddition(currentNode, "-}\n", Format.ALLTIMES));
			} else if (nodeName.equalsIgnoreCase("td")) {
				toCheckEnd.add(new EndAddition(currentNode, '\t', Format.NOADDLAST));
			} else if (nodeName.equalsIgnoreCase("tr")) {
				sb.append('\n');
			} else if (nodeName.equalsIgnoreCase("br")) {
				sb.append('\n');
			} else if (nodeName.equalsIgnoreCase("div")) {
				sb.append('\n');
			} else if (nodeName.equalsIgnoreCase("h1")) {
				sb.append("\n${");
				toCheckEnd.add(new EndAddition(currentNode, "}$\n", Format.ALLTIMES));
			} else if (nodeName.equalsIgnoreCase("h2")) {
				sb.append("\n${");
				toCheckEnd.add(new EndAddition(currentNode, "}$\n", Format.ALLTIMES));
			} else if (nodeName.equalsIgnoreCase("h3")) {
				sb.append("\n${");
				toCheckEnd.add(new EndAddition(currentNode, "}$\n", Format.ALLTIMES));
			} else if (nodeName.equalsIgnoreCase("h4")) {
				sb.append("\n${");
				toCheckEnd.add(new EndAddition(currentNode, "}$\n", Format.ALLTIMES));
			} else if (nodeName.equalsIgnoreCase("h5")) {
				sb.append("\n${");
				toCheckEnd.add(new EndAddition(currentNode, "}$\n", Format.ALLTIMES));
			} else if (nodeName.equalsIgnoreCase("h6")) {
				sb.append("\n${");
				toCheckEnd.add(new EndAddition(currentNode, "}$\n", Format.ALLTIMES));
			} else if (nodeName.equalsIgnoreCase("option")) {
				sb.append('\n');
			} else if (nodeName.equalsIgnoreCase("li")) {
				sb.append('\n');
			} else if (nodeName.equalsIgnoreCase("p")) {
				sb.append('\n');
				toCheckEnd.add(new EndAddition(currentNode, '\n', Format.ALLTIMES));
			} else if (nodeName.equalsIgnoreCase("img")) {
				Node attSrc = currentNode.getAttributes().getNamedItem("src");
				if (attSrc != null) {
					sb.append("<img src=\"" + attSrc.getNodeValue() + "\" />");
				}
			}

			else if (nodeName.equalsIgnoreCase("param")) {
				// Capturing videos

				// Make sure its a flash object
				Node attName = currentNode.getAttributes().getNamedItem("name");
				if (attName != null && attName.getNodeValue().equalsIgnoreCase("flashvars")) {
					// try to extract video information when available
					Node attValue = currentNode.getAttributes().getNamedItem("value");
					if (attValue != null) {
						String videoFile = null;
						String thumbnailFile = null;

						boolean surePlayer = false;

						Node attParentData = currentNode.getParentNode().getAttributes().getNamedItem("data");
						if (attParentData != null) {
							if (attParentData.getNodeValue().contains("player") || attParentData.getNodeValue().contains("Player")) {
								surePlayer = true;
							}

							String[] pairs = attValue.getNodeValue().split("&");
							for (String pair : pairs) {
								String[] split = pair.split("=");

								if (videoFile == null
										&& (containsFileExt(split[1], videoExt) || split[0].contains("video") || (surePlayer && split[0]
												.contains("file")))) {
									videoFile = pair.substring(pair.indexOf('=') + 1);
								} else if (thumbnailFile == null && containsFileExt(split[1], imageExt)) {
									thumbnailFile = pair.substring(pair.indexOf('=') + 1);
								}
							}

							if (videoFile != null) {
								sb.append("<video file=\"" + videoFile + "\" thumbnail=\"" + thumbnailFile + "\" />");
							}
						}
					}
				}
			} else if (nodeName.equalsIgnoreCase("a")) {
				sb.append('^');
				toCheckEnd.add(new EndAddition(currentNode, '^', Format.ALLTIMES));
			} else if (nodeType == Node.COMMENT_NODE) {
				String comment = currentNode.getTextContent();
				if (comment.contains("Content")) {
					if (comment.contains("Start")) {
						sb.append("\n//$$StartContent$$//\n");
					} else if (comment.contains("End")) {
						sb.append("\n//$$EndContent$$//\n");
					}
				}
			}
			if (nodeType == Node.COMMENT_NODE) {
				// walker.skipChildren();
			}
			if (nodeType == Node.TEXT_NODE) {
				// cleanup and trim the value
				String text = currentNode.getNodeValue();
				text = text.replaceAll("\\s+", " ");
				text = text.trim();
				if (text.length() > 0) {
					if (sb.length() > 0)
						sb.append(' ');
					sb.append(text);
				}
			}

			if (currentNode.getNextSibling() == null && !toCheckEnd.isEmpty()) {
				if (currentNode.getParentNode() == toCheckEnd.peek().node) {
					EndAddition n = toCheckEnd.pop();
					if (!(n.format == Format.NOADDLAST && n.node.getNextSibling() == null)) {
						// if(sb.charAt(sb.length() - 1) == '\n')
						// {
						// if(n.toAdd != '\t' && n.toAdd != ' ' && !(n.toAdd ==
						// '\n' && sb.charAt(sb.length() - 2) == '\n'))
						sb.append(n.toAdd);
						// }
					}
				}
			}
		}

		return abort;
	}

	private boolean containsFileExt(String filename, String[] fileExtentions) {
		for (String ext : fileExtentions) {
			if (filename.contains(ext)) {
				return true;
			}
		}
		return false;
	}

	public static class EndAddition {
		public Node node;
		public String toAdd;
		public Format format;

		public static enum Format {
			ALLTIMES, NOADDLAST
		}

		public EndAddition(Node node, String toAdd, Format format) {
			this.node = node;
			this.toAdd = toAdd;
			this.format = format;
		}

		public EndAddition(Node node, char toAdd, Format format) {
			this.node = node;
			this.toAdd = toAdd + "";
			this.format = format;
		}
	}

	/**
	 * This method takes a {@link StringBuffer} and a DOM {@link Node}, and will
	 * append the content text found beneath the first <code>title</code> node
	 * to the <code>StringBuffer</code>.
	 * 
	 * @return true if a title node was found, false otherwise
	 */
	public boolean getTitle(StringBuilder sb, Node node) {

		NodeWalker walker = new NodeWalker(node);

		while (walker.hasNext()) {

			Node currentNode = walker.nextNode();
			String nodeName = currentNode.getNodeName();
			short nodeType = currentNode.getNodeType();

			/*
			 * if ("body".equalsIgnoreCase(nodeName)) { // stop after HEAD
			 * return false; }
			 */

			if (nodeType == Node.ELEMENT_NODE) {
				if ("title".equalsIgnoreCase(nodeName)) {
					getText(sb, currentNode);
					return true;
				}
			}
		}

		return false;
	}

	/** If Node contains a BASE tag then it's HREF is returned. */
	public URL getBase(Node node) {

		NodeWalker walker = new NodeWalker(node);

		while (walker.hasNext()) {

			Node currentNode = walker.nextNode();
			String nodeName = currentNode.getNodeName();
			short nodeType = currentNode.getNodeType();

			// is this node a BASE tag?
			if (nodeType == Node.ELEMENT_NODE) {

				if ("body".equalsIgnoreCase(nodeName)) { // stop after HEAD
					return null;
				}

				if ("base".equalsIgnoreCase(nodeName)) {
					NamedNodeMap attrs = currentNode.getAttributes();
					for (int i = 0; i < attrs.getLength(); i++) {
						Node attr = attrs.item(i);
						if ("href".equalsIgnoreCase(attr.getNodeName())) {
							try {
								return new URL(attr.getNodeValue());
							} catch (MalformedURLException e) {
							}
						}
					}
				}
			}
		}

		// no.
		return null;
	}

	private boolean hasOnlyWhiteSpace(Node node) {
		String val = node.getNodeValue();
		for (int i = 0; i < val.length(); i++) {
			if (!Character.isWhitespace(val.charAt(i)))
				return false;
		}
		return true;
	}

	// this only covers a few cases of empty links that are symptomatic
	// of nekohtml's DOM-fixup process...
	private boolean shouldThrowAwayLink(Node node, NodeList children, int childLen, LinkParams params) {
		if (childLen == 0) {
			// this has no inner structure
			if (params.childLen == 0)
				return false;
			else
				return true;
		} else if ((childLen == 1) && (children.item(0).getNodeType() == Node.ELEMENT_NODE)
				&& (params.elName.equalsIgnoreCase(children.item(0).getNodeName()))) {
			// single nested link
			return true;

		} else if (childLen == 2) {

			Node c0 = children.item(0);
			Node c1 = children.item(1);

			if ((c0.getNodeType() == Node.ELEMENT_NODE) && (params.elName.equalsIgnoreCase(c0.getNodeName())) && (c1.getNodeType() == Node.TEXT_NODE)
					&& hasOnlyWhiteSpace(c1)) {
				// single link followed by whitespace node
				return true;
			}

			if ((c1.getNodeType() == Node.ELEMENT_NODE) && (params.elName.equalsIgnoreCase(c1.getNodeName())) && (c0.getNodeType() == Node.TEXT_NODE)
					&& hasOnlyWhiteSpace(c0)) {
				// whitespace node followed by single link
				return true;
			}

		} else if (childLen == 3) {
			Node c0 = children.item(0);
			Node c1 = children.item(1);
			Node c2 = children.item(2);

			if ((c1.getNodeType() == Node.ELEMENT_NODE) && (params.elName.equalsIgnoreCase(c1.getNodeName())) && (c0.getNodeType() == Node.TEXT_NODE)
					&& (c2.getNodeType() == Node.TEXT_NODE) && hasOnlyWhiteSpace(c0) && hasOnlyWhiteSpace(c2)) {
				// single link surrounded by whitespace nodes
				return true;
			}
		}

		return false;
	}

	/**
	 * Handles cases where the url param information is encoded into the base
	 * url as opposed to the target.
	 * <p>
	 * If the taget contains params (i.e. ';xxxx') information then the target
	 * params information is assumed to be correct and any base params
	 * information is ignored. If the base contains params information but the
	 * tareget does not, then the params information is moved to the target
	 * allowing it to be correctly determined by the java.net.URL class.
	 * 
	 * @param base
	 *            The base URL.
	 * @param target
	 *            The target path from the base URL.
	 * 
	 * @return URL A URL with the params information correctly encoded.
	 * 
	 * @throws MalformedURLException
	 *             If the url is not a well formed URL.
	 */
	private URL fixEmbeddedParams(URL base, String target) throws MalformedURLException {

		// the target contains params information or the base doesn't then no
		// conversion necessary, return regular URL
		if (target.indexOf(';') >= 0 || base.toString().indexOf(';') == -1) {
			return new URL(base, target);
		}

		// get the base url and it params information
		String baseURL = base.toString();
		int startParams = baseURL.indexOf(';');
		String params = baseURL.substring(startParams);

		// if the target has a query string then put the params information
		// after
		// any path but before the query string, otherwise just append to the
		// path
		int startQS = target.indexOf('?');
		if (startQS >= 0) {
			target = target.substring(0, startQS) + params + target.substring(startQS);
		} else {
			target += params;
		}

		return new URL(base, target);
	}

	/**
	 * This method finds all anchors below the supplied DOM <code>node</code>,
	 * and creates appropriate {@link Outlink} records for each (relative to the
	 * supplied <code>base</code> URL), and adds them to the
	 * <code>outlinks</code> {@link ArrayList}.
	 * 
	 * <p>
	 * 
	 * Links without inner structure (tags, text, etc) are discarded, as are
	 * links which contain only single nested links and empty text nodes (this
	 * is a common DOM-fixup artifact, at least with nekohtml).
	 */
	public void getOutlinks(URL base, ArrayList<Outlink> outlinks, Node node) {

		NodeWalker walker = new NodeWalker(node);
		while (walker.hasNext()) {

			Node currentNode = walker.nextNode();
			String nodeName = currentNode.getNodeName();
			short nodeType = currentNode.getNodeType();
			NodeList children = currentNode.getChildNodes();
			int childLen = (children != null) ? children.getLength() : 0;

			if (nodeType == Node.ELEMENT_NODE) {

				nodeName = nodeName.toLowerCase();
				LinkParams params = linkParams.get(nodeName);
				if (params != null) {
					if (!shouldThrowAwayLink(currentNode, children, childLen, params)) {

						StringBuilder linkText = new StringBuilder();
						getText(linkText, currentNode, true);

						NamedNodeMap attrs = currentNode.getAttributes();
						String target = null;
						boolean noFollow = false;
						boolean post = false;
						for (int i = 0; i < attrs.getLength(); i++) {
							Node attr = attrs.item(i);
							String attrName = attr.getNodeName();
							if (params.attrName.equalsIgnoreCase(attrName)) {
								target = attr.getNodeValue();
							} else if ("rel".equalsIgnoreCase(attrName) && "nofollow".equalsIgnoreCase(attr.getNodeValue())) {
								noFollow = true;
							} else if ("method".equalsIgnoreCase(attrName) && "post".equalsIgnoreCase(attr.getNodeValue())) {
								post = true;
							}
						}
						if (target != null && !noFollow && !post)
							try {

								URL url = (base.toString().indexOf(';') > 0) ? fixEmbeddedParams(base, target) : new URL(base, target);
								
								Outlink outlink = new Outlink(url.toString(), linkText.toString().trim());
								outlinks.add(outlink);
							} catch (MalformedURLException e) {
								// don't care
							}
					}
					// this should not have any children, skip them
					if (params.childLen == 0)
						continue;
				}
			}
		}
	}

}
