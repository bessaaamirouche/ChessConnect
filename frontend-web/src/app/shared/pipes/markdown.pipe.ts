import { Pipe, PipeTransform, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { marked } from 'marked';

@Pipe({
  name: 'markdown',
  standalone: true
})
export class MarkdownPipe implements PipeTransform {
  private readonly platformId = inject(PLATFORM_ID);

  // Allowed HTML tags for sanitization (whitelist approach)
  private readonly allowedTags = new Set([
    'p', 'br', 'strong', 'b', 'em', 'i', 'u', 's', 'del',
    'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
    'ul', 'ol', 'li', 'blockquote', 'pre', 'code',
    'a', 'img', 'table', 'thead', 'tbody', 'tr', 'th', 'td',
    'hr', 'span', 'div'
  ]);

  private readonly allowedAttributes: Record<string, Set<string>> = {
    'a': new Set(['href', 'title', 'target', 'rel']),
    'img': new Set(['src', 'alt', 'title', 'width', 'height']),
    'td': new Set(['colspan', 'rowspan']),
    'th': new Set(['colspan', 'rowspan'])
  };

  // Dangerous protocols to block
  private readonly dangerousProtocols = [
    'javascript:',
    'data:',
    'vbscript:',
    'file:'
  ];

  constructor() {
    // Configure marked options
    marked.setOptions({
      breaks: true,
      gfm: true
    });
  }

  transform(value: string | null | undefined): string {
    if (!value) {
      return '';
    }

    const html = marked.parse(value) as string;
    return this.sanitizeHtml(html);
  }

  private sanitizeHtml(html: string): string {
    // Use DOMParser for safer parsing (doesn't execute scripts)
    if (!isPlatformBrowser(this.platformId)) {
      // On server-side, return escaped text only
      return this.escapeHtml(html);
    }

    const parser = new DOMParser();
    const doc = parser.parseFromString(html, 'text/html');

    // Sanitize the parsed document
    this.sanitizeNode(doc.body);

    return doc.body.innerHTML;
  }

  private sanitizeNode(node: Node): void {
    const nodesToRemove: Node[] = [];

    node.childNodes.forEach(child => {
      if (child.nodeType === Node.ELEMENT_NODE) {
        const element = child as Element;
        const tagName = element.tagName.toLowerCase();

        // Remove disallowed tags entirely
        if (!this.allowedTags.has(tagName)) {
          nodesToRemove.push(child);
          return;
        }

        // Remove disallowed attributes
        const allowedAttrs = this.allowedAttributes[tagName] || new Set();
        const attrsToRemove: string[] = [];

        for (const attr of Array.from(element.attributes)) {
          const attrName = attr.name.toLowerCase();
          const attrValue = attr.value.toLowerCase().trim();

          // Always remove event handlers (onclick, onerror, onload, etc.)
          if (attrName.startsWith('on')) {
            attrsToRemove.push(attr.name);
            continue;
          }

          // Check for dangerous protocols in any attribute
          if (this.containsDangerousProtocol(attrValue)) {
            attrsToRemove.push(attr.name);
            continue;
          }

          // Remove attributes not in whitelist (except class and id)
          if (!allowedAttrs.has(attrName) && !['class', 'id'].includes(attrName)) {
            attrsToRemove.push(attr.name);
          }
        }

        attrsToRemove.forEach(attr => element.removeAttribute(attr));

        // Additional validation for links
        if (tagName === 'a') {
          const href = element.getAttribute('href');
          if (href) {
            const hrefLower = href.toLowerCase().trim();
            // Remove href if it contains dangerous protocols
            if (this.containsDangerousProtocol(hrefLower)) {
              element.removeAttribute('href');
            }
          }
          // Always add security attributes to links
          element.setAttribute('rel', 'noopener noreferrer');
        }

        // Additional validation for images
        if (tagName === 'img') {
          const src = element.getAttribute('src');
          if (src) {
            const srcLower = src.toLowerCase().trim();
            // Only allow http, https, and relative URLs
            if (this.containsDangerousProtocol(srcLower)) {
              element.removeAttribute('src');
            }
          }
        }

        // Recursively sanitize children
        this.sanitizeNode(element);
      } else if (child.nodeType === Node.COMMENT_NODE) {
        // Remove HTML comments (could contain conditional IE attacks)
        nodesToRemove.push(child);
      }
    });

    nodesToRemove.forEach(n => node.removeChild(n));
  }

  private containsDangerousProtocol(value: string): boolean {
    const normalized = value.replace(/\s/g, '').toLowerCase();
    return this.dangerousProtocols.some(protocol => normalized.startsWith(protocol));
  }

  private escapeHtml(text: string): string {
    const div = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, char => div[char as keyof typeof div]);
  }
}
