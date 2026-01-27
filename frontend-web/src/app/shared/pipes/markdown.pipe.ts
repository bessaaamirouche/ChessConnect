import { Pipe, PipeTransform } from '@angular/core';
import { marked } from 'marked';

@Pipe({
  name: 'markdown',
  standalone: true
})
export class MarkdownPipe implements PipeTransform {
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
    // Create a temporary DOM element to parse HTML
    const template = document.createElement('template');
    template.innerHTML = html;

    this.sanitizeNode(template.content);

    return template.innerHTML;
  }

  private sanitizeNode(node: Node): void {
    const nodesToRemove: Node[] = [];

    node.childNodes.forEach(child => {
      if (child.nodeType === Node.ELEMENT_NODE) {
        const element = child as Element;
        const tagName = element.tagName.toLowerCase();

        // Remove disallowed tags
        if (!this.allowedTags.has(tagName)) {
          nodesToRemove.push(child);
          return;
        }

        // Remove disallowed attributes
        const allowedAttrs = this.allowedAttributes[tagName] || new Set();
        const attrsToRemove: string[] = [];

        for (const attr of Array.from(element.attributes)) {
          // Always remove event handlers and javascript: URLs
          if (attr.name.startsWith('on') ||
              attr.value.toLowerCase().includes('javascript:')) {
            attrsToRemove.push(attr.name);
          } else if (!allowedAttrs.has(attr.name) && !['class', 'id'].includes(attr.name)) {
            attrsToRemove.push(attr.name);
          }
        }

        attrsToRemove.forEach(attr => element.removeAttribute(attr));

        // For links, ensure they're safe
        if (tagName === 'a') {
          const href = element.getAttribute('href');
          if (href && (href.startsWith('javascript:') || href.startsWith('data:'))) {
            element.removeAttribute('href');
          }
          // Add security attributes to external links
          element.setAttribute('rel', 'noopener noreferrer');
        }

        // Recursively sanitize children
        this.sanitizeNode(element);
      }
    });

    nodesToRemove.forEach(n => node.removeChild(n));
  }
}
