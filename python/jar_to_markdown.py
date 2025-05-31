import argparse
import zipfile
import os
import re
import glob

def get_java_description(file_content_str):
    # Try to find Javadoc for the first public class
    javadoc_match = re.search(r"/\*\*(.*?)\*/\s*public\s+class", file_content_str, re.DOTALL)
    if javadoc_match:
        javadoc_content = javadoc_match.group(1)
        lines = javadoc_content.split('\n')
        cleaned_lines = []
        for line in lines:
            cleaned_line = line.strip()
            if cleaned_line.startswith('*'):
                cleaned_line = cleaned_line[1:].strip()
            if cleaned_line:
                 cleaned_lines.append(cleaned_line)
        return "\n".join(cleaned_lines)
    return ""

def get_language(filename):
    if filename.endswith(".java"): return "java"
    if filename.endswith(".xml"): return "xml"
    if filename.endswith(".properties"): return "properties"
    if filename.endswith(".json"): return "json"
    if filename.endswith(".js"): return "javascript"
    if filename.endswith(".ts"): return "typescript"
    if filename.endswith(".py"): return "python"
    if filename.endswith(".sh") or filename.endswith(".bash"): return "bash"
    if filename.endswith(".txt"): return "text"
    return "unknown"

def process_jar(jar_path, output_dir_processed_markdown):
    print(f"Starting Part 1: Processing JAR '{jar_path}' into '{output_dir_processed_markdown}'")
    if not os.path.exists(jar_path):
        print(f"Error: JAR file not found at {jar_path}")
        exit(1)

    if not os.path.exists(output_dir_processed_markdown):
        print(f"Creating output directory for processed markdown: {output_dir_processed_markdown}")
        os.makedirs(output_dir_processed_markdown)

    processed_files_count = 0
    try:
        with zipfile.ZipFile(jar_path, 'r') as jar_file:
            for member_info in jar_file.infolist():
                if member_info.is_dir():
                    continue

                filename = member_info.filename

                if filename.endswith(".class") or filename.endswith(".md"):
                    continue

                if filename.startswith("META-INF/") or filename.endswith((".DSA", ".SF", ".RSA")): # Expanded common signature files
                    continue

                try:
                    with jar_file.open(member_info) as file_in_jar:
                        try:
                            file_content_str = file_in_jar.read().decode('utf-8')
                        except UnicodeDecodeError:
                            try:
                                file_content_str = file_in_jar.read().decode('latin-1') # Common fallback
                            except Exception as e_decode_fallback:
                                print(f"Warning: Could not decode {filename} as UTF-8 or Latin-1: {e_decode_fallback}. Skipping.")
                                continue

                        language = get_language(filename)
                        description = ""
                        if language == "java":
                            description = get_java_description(file_content_str)

                        title_base = os.path.splitext(os.path.basename(filename))[0]
                        title = f"Content of {title_base} from {os.path.basename(jar_path)}"

                        md_content = f"TITLE: {title}\n"
                        if description:
                            md_content += f"DESCRIPTION: {description}\n"
                        md_content += f"SOURCE: {filename} (in {os.path.basename(jar_path)})\n"
                        md_content += f"LANGUAGE: {language}\n"
                        md_content += "CODE:\n"
                        md_content += "```" + (language if language != "unknown" else "") + "\n" # Avoid unknown language in code block
                        md_content += file_content_str.strip() + "\n"
                        md_content += "```\n"
                        md_content += "\n----------------------------------------\n"

                        output_filename_base = filename.replace('/', '_').replace('\\', '_')
                        # Sanitize further if needed, e.g. for other invalid path characters
                        output_filename_base = re.sub(r'[^a-zA-Z0-9_.-]', '_', output_filename_base)
                        output_filepath = os.path.join(output_dir_processed_markdown, output_filename_base + ".md")

                        with open(output_filepath, "w", encoding="utf-8") as md_file:
                            md_file.write(md_content)
                        processed_files_count +=1

                except Exception as e_file_processing:
                    print(f"Error processing file {filename} in JAR: {e_file_processing}. Skipping this file.")
                    continue
        print(f"Part 1: Processed {processed_files_count} files from JAR into individual Markdown files in '{output_dir_processed_markdown}'.")

    except FileNotFoundError:
        print(f"Error: Input JAR file '{jar_path}' not found.")
        exit(1)
    except zipfile.BadZipFile:
        print(f"Error: Could not read JAR file '{jar_path}'. It may be corrupted or not a valid JAR.")
        exit(1)
    except Exception as e:
        print(f"An unexpected error occurred during JAR processing: {e}")
        exit(1)

def merge_markdown_files(jar_path, processed_markdown_dir, merged_output_file):
    print(f"Starting Part 2: Merging Markdown files into '{merged_output_file}'")
    merged_content = ""

    # 1. Add processed markdown files
    print(f"Looking for processed markdown files in: {processed_markdown_dir}")
    processed_md_files = glob.glob(os.path.join(processed_markdown_dir, "*.md"))
    print(f"Found {len(processed_md_files)} processed markdown files.")
    for md_file_path in processed_md_files:
        try:
            with open(md_file_path, 'r', encoding='utf-8') as f:
                merged_content += f.read() + "\n\n" # Add extra newline for separation
        except Exception as e:
            print(f"Error reading processed markdown file {md_file_path}: {e}. Skipping.")
            continue

    # 2. Add original markdown files from the JAR
    print(f"Extracting original Markdown files from JAR '{jar_path}'")
    original_md_count = 0
    try:
        with zipfile.ZipFile(jar_path, 'r') as jar_file:
            for member_info in jar_file.infolist():
                if member_info.is_dir() or not member_info.filename.lower().endswith(".md"):
                    continue

                filename = member_info.filename
                print(f"Found original Markdown file in JAR: {filename}")
                try:
                    with jar_file.open(member_info) as file_in_jar:
                        try:
                            original_md_content = file_in_jar.read().decode('utf-8')
                        except UnicodeDecodeError:
                            try:
                                original_md_content = file_in_jar.read().decode('latin-1')
                            except Exception as e_decode_fallback:
                                print(f"Warning: Could not decode original markdown {filename}: {e_decode_fallback}. Skipping.")
                                continue

                        # Optionally, wrap original MDs with some context, or add as is
                        title = f"Original Markdown: {os.path.basename(filename)} from {os.path.basename(jar_path)}"
                        source = f"{filename} (original Markdown in {os.path.basename(jar_path)})"

                        merged_content += f"TITLE: {title}\n"
                        merged_content += f"SOURCE: {source}\n"
                        merged_content += "LANGUAGE: markdown\n" # Explicitly set language
                        merged_content += "CODE:\n"
                        merged_content += "```markdown\n"
                        merged_content += original_md_content.strip() + "\n"
                        merged_content += "```\n"
                        merged_content += "\n----------------------------------------\n"
                        original_md_count +=1
                except Exception as e_orig_md:
                    print(f"Error processing original markdown file {filename} from JAR: {e_orig_md}. Skipping.")
                    continue
        print(f"Added {original_md_count} original Markdown files from the JAR.")
    except Exception as e_jar_orig_md:
        print(f"Error accessing original markdown files in JAR {jar_path}: {e_jar_orig_md}")
        # Continue without original MDs if JAR processing fails here, or exit(1)

    try:
        with open(merged_output_file, "w", encoding="utf-8") as f_out:
            f_out.write(merged_content)
        print(f"Part 2: Successfully merged all Markdown content into '{merged_output_file}'.")
    except Exception as e_merge_write:
        print(f"Error writing merged markdown file '{merged_output_file}': {e_merge_write}")
        exit(1)


def main():
    parser = argparse.ArgumentParser(description="Process a JAR file, convert its contents to Markdown, and merge them.")
    parser.add_argument("jar_path", help="Path to the input JAR file.")
    parser.add_argument("output_dir_processed_markdown", help="Directory to save the processed markdown files from JAR contents.")
    parser.add_argument("merged_output_file", help="Path to save the final merged Markdown file.")

    args = parser.parse_args()

    # Part 1: Process JAR contents into individual markdown files
    process_jar(args.jar_path, args.output_dir_processed_markdown)

    # Part 2: Merge processed markdown files and original markdown files from JAR
    merge_markdown_files(args.jar_path, args.output_dir_processed_markdown, args.merged_output_file)

    print("Script finished.")

if __name__ == "__main__":
    main()
