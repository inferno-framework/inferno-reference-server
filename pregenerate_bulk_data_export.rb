require 'base64'
require 'faraday'
require 'json'

BASE_URL = "http://localhost:8080/reference-server/r4"

GROUP_ID = "1a"

# 1. Do a real export on the server

connection = Faraday.new do |f|
  f.headers['Authorization'] = "Bearer SAMPLE_TOKEN"
end

response = connection.get("#{BASE_URL}/Group/#{GROUP_ID}/$export", nil, { "Prefer": "respond-async", "X-Override-Interceptor": "true"})
unless response.success?
  puts response.inspect
  exit
end

poll_status_url = response.headers['content-location']
puts "poll status url #{poll_status_url}"

# 2. Get the status until it's ready
loop do
  response = connection.get(poll_status_url)

  if response.status == 202
    wait_time = Integer(response.headers['retry-after'])
    puts "sleeping #{wait_time}"
    sleep(wait_time)
    next
  elsif response.status == 200
    break
  else
    puts response.inspect
    exit
  end
end


# {
#   "transactionTime": "2024-11-07T12:34:53.487+00:00",
#   "request": "https://inferno.healthit.gov/reference-server/r4/Group/1a/$export",
#   "requiresAccessToken": true,
#   "output": [
#     {
#       "type": "Condition",
#       "url": "https://inferno.healthit.gov/reference-server/r4/Binary/zdTotLo91lr4sxt54LjDNiImeIhgG3xd"
#     },
#     ...
#   ],
#   "error": []
# }

# response = connection.get("https://inferno.healthit.gov/reference-server/r4/$export-poll-status?_jobId=60218382-dc61-46d6-a739-742358643192")

result = JSON.parse(response.body)

binaries = []

# 3. Iterate over the output, capture the Binaries

result['output'].each do |output_file|
  response = connection.get(output_file['url'], nil, { "Accept": "application/fhir+json"})
  binary = JSON.parse(response.body)
  binaries.push(binary)
end


# https://inferno.healthit.gov/reference-server/r4/Binary/zdTotLo91lr4sxt54LjDNiImeIhgG3xd
# with header
# Accept: application/fhir+json
# =>
# {
#   "resourceType": "Binary",
#   "id": "zdTotLo91lr4sxt54LjDNiImeIhgG3xd",
#   "meta": {
#     "extension": [
#       {
#         "url": "https://hapifhir.org/NamingSystem/bulk-export-job-id",
#         "valueString": "b39ed20b-1f3b-4fca-84c5-7ff21e4c2237"
#       },
#       {
#         "url": "https://hapifhir.org/NamingSystem/bulk-export-binary-resource-type",
#         "valueString": "Condition"
#       }
#     ],
#     "versionId": "1",
#     "lastUpdated": "2024-11-07T12:33:53.843+00:00"
#   },
#   "contentType": "application/fhir+ndjson",
#   "data": "..."
# }

# 4. Get all the Location resources and manually construct the Binary for it

response = connection.get("#{BASE_URL}/Location")
bundle = JSON.parse(response.body)
resources = bundle['entry'].map { |e| e['resource'] }
ndjson = resources.map { |r| JSON.generate(r) }.join("\n")

# HAPI uses a 32-char random string generated using a SecureRandom
ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
random_id = Array.new(32) { ALPHANUMERIC[rand(62)] }.join

location_binary = {
  "resourceType" => "Binary",
  "id" => random_id,
  "meta" => {
    "extension" => [
      {
        "url" => "https://hapifhir.org/NamingSystem/bulk-export-job-id",
        "valueString" => "b39ed20b-1f3b-4fca-84c5-7ff21e4c2237"
      },
      {
        "url" => "https://hapifhir.org/NamingSystem/bulk-export-binary-resource-type",
        "valueString" => "Location"
      }
    ],
    "versionId" => "1",
    "lastUpdated" => "2024-11-07T12:33:53.843+00:00"
  },
  "contentType" => "application/fhir+ndjson",
  "data" => Base64.strict_encode64(ndjson)  # strict_encode because plain encode adds newlines
}

binaries.push(location_binary)

# # 5. Wrap all of the Binaries in a Bundle, with PUT actions so the IDs are fixed

bundle = {
  "resourceType" => "Bundle",
  "meta" => {
    "source" => 'https://github.com/inferno-framework/inferno-reference-server',
    "tag" => [{
      "system" => "group-id",
      "code" => GROUP_ID
    }]
  },
  "type" => "transaction",
  "entry" => binaries.map do |binary|
    {
      "resource" => binary,
      "request" => {
        "method" => "PUT",
        "url" => "Binary/#{binary['id']}"
      }
    }
  end
}

path = 'resources/cached_bulk_data_export.json'
File.open(path, 'w') do |f|
  f.write(JSON.pretty_generate(bundle))
end

puts "wrote bundle to #{path}"
